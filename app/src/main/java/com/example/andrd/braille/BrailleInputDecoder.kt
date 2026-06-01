package com.example.andrd.braille

/**
 * Decodes raw bytes arriving from a Braille display's Bluetooth input stream.
 *
 * ## Wire format
 *
 * Most Bluetooth Braille displays that use the Serial Port Profile send
 * keypress data as single bytes or short byte sequences. This decoder handles
 * the common subset shared by many displays:
 *
 *   Braille dot key:
 *     A byte whose lower 6 bits encode which dots are depressed simultaneously.
 *     Bit 0 = dot 1, bit 1 = dot 2, …, bit 5 = dot 6.
 *     The byte is sent when all dots are released (chord entry mode).
 *     Example: letter "a" (dot 1 only) → 0x01
 *              letter "b" (dots 1+2)  → 0x03
 *              space (no dots)        → 0x00
 *
 *   Navigation / command keys:
 *     Bytes with the high nibble set (0x40 and above) are treated as
 *     command keys. Common values across many display families:
 *       0x40  pan right  (scroll display one page right)
 *       0x41  pan left   (scroll display one page left)
 *       0x42  scroll up  / previous item
 *       0x43  scroll down / next item
 *       0x44  home / top
 *       0x45  activate / enter / routing key (generic)
 *       0x46  back / escape
 *
 * Displays that use a different protocol (e.g. BRLTTY key tables or
 * proprietary Freedom Scientific / HumanWare protocols) will need
 * additional decoder classes. This decoder is a compatible baseline.
 */
object BrailleInputDecoder {

    // ── Dot-pattern → character table ─────────────────────────────────────
    // Reverse of BrailleTranslator: 6-bit dot mask → letter (lowercase).
    // Bits:  5=dot6  4=dot5  3=dot4  2=dot3  1=dot2  0=dot1
    private val dotPatternToLetter: Map<Int, Char> = mapOf(
        0x01 to 'a', // 000001  dot 1
        0x03 to 'b', // 000011  dots 1 2
        0x09 to 'c', // 001001  dots 1 4
        0x19 to 'd', // 011001  dots 1 4 5
        0x11 to 'e', // 010001  dots 1 5
        0x0B to 'f', // 001011  dots 1 2 4
        0x1B to 'g', // 011011  dots 1 2 4 5
        0x13 to 'h', // 010011  dots 1 2 5
        0x0A to 'i', // 001010  dots 2 4
        0x1A to 'j', // 011010  dots 2 4 5
        0x05 to 'k', // 000101  dots 1 3
        0x07 to 'l', // 000111  dots 1 2 3
        0x0D to 'm', // 001101  dots 1 3 4
        0x1D to 'n', // 011101  dots 1 3 4 5
        0x15 to 'o', // 010101  dots 1 3 5
        0x0F to 'p', // 001111  dots 1 2 3 4
        0x1F to 'q', // 011111  dots 1 2 3 4 5
        0x17 to 'r', // 010111  dots 1 2 3 5
        0x0E to 's', // 001110  dots 2 3 4
        0x1E to 't', // 011110  dots 2 3 4 5
        0x25 to 'u', // 100101  dots 1 3 6
        0x27 to 'v', // 100111  dots 1 2 3 6
        0x3A to 'w', // 111010  dots 2 4 5 6
        0x2D to 'x', // 101101  dots 1 3 4 6
        0x3D to 'y', // 111101  dots 1 3 4 5 6
        0x35 to 'z'  // 110101  dots 1 3 5 6
    )

    // Number indicator: dots 3 4 5 6 → 0x3C
    private const val NUMBER_INDICATOR_PATTERN = 0x3C

    // Capital indicator: dot 6 → 0x20
    private const val CAPITAL_INDICATOR_PATTERN = 0x20

    // Number indicator maps the same letter-patterns to digits
    // Letters a–j map to digits 1–9,0
    private val letterToDigit: Map<Char, Char> = mapOf(
        'a' to '1', 'b' to '2', 'c' to '3', 'd' to '4', 'e' to '5',
        'f' to '6', 'g' to '7', 'h' to '8', 'i' to '9', 'j' to '0'
    )

    // Punctuation patterns
    private val dotPatternToPunct: Map<Int, Char> = mapOf(
        0x02 to ',',  // dot 2
        0x06 to ';',  // dots 2 3
        0x12 to ':',  // dots 2 5
        0x32 to '.',  // dots 2 5 6
        0x16 to '!',  // dots 2 3 5
        0x26 to '?',  // dots 2 3 6
        0x04 to '\'', // dot 3
        0x24 to '-',  // dots 3 6
        0x36 to '(',  // dots 2 3 5 6
        0x0C to '/'   // dots 3 4
    )

    // ── Command byte constants ─────────────────────────────────────────────
    const val CMD_PAN_RIGHT   = 0x40
    const val CMD_PAN_LEFT    = 0x41
    const val CMD_SCROLL_UP   = 0x42
    const val CMD_SCROLL_DOWN = 0x43
    const val CMD_HOME        = 0x44
    const val CMD_ACTIVATE    = 0x45
    const val CMD_BACK        = 0x46

    // ── Decode result ──────────────────────────────────────────────────────

    sealed class DecodeResult {
        /** A printable character (letter, digit, punctuation, or space). */
        data class Character(val char: Char) : DecodeResult()
        /** A backspace — delete one character behind the cursor. */
        object Backspace : DecodeResult()
        /** A navigation or command action. */
        data class Command(val code: Int) : DecodeResult()
        /** A byte we could not interpret — ignore it. */
        object Unknown : DecodeResult()
    }

    // ── Stateful decoder ───────────────────────────────────────────────────

    /**
     * Stateful decoder instance. One instance per connection — it remembers
     * whether the previous byte set the capital or number indicator.
     */
    class Decoder {
        private var nextIsCapital = false
        private var inNumberContext = false

        /**
         * Decodes one byte from the display's input stream.
         * Call this for every byte received, in order.
         */
        fun decode(byte: Int): DecodeResult {
            val masked = byte and 0xFF

            // Command key (high nibble set)
            if (masked >= 0x40) {
                nextIsCapital = false
                inNumberContext = false
                return if (masked == CMD_PAN_LEFT || masked == CMD_PAN_RIGHT ||
                           masked == CMD_SCROLL_UP || masked == CMD_SCROLL_DOWN ||
                           masked == CMD_HOME || masked == CMD_ACTIVATE ||
                           masked == CMD_BACK) {
                    DecodeResult.Command(masked)
                } else {
                    DecodeResult.Unknown
                }
            }

            // Space
            if (masked == 0x00) {
                inNumberContext = false
                return DecodeResult.Character(' ')
            }

            // Backspace: dots 1 2 (pattern 0x03) is often used as backspace
            // on displays that support it; check for the dedicated BS pattern
            // 0x08 (dot 4 alone, used on some Refreshabraille displays) too.
            if (masked == 0x08) {
                return DecodeResult.Backspace
            }

            // Capital indicator
            if (masked == CAPITAL_INDICATOR_PATTERN) {
                nextIsCapital = true
                return DecodeResult.Unknown // consume — wait for next byte
            }

            // Number indicator
            if (masked == NUMBER_INDICATOR_PATTERN) {
                inNumberContext = true
                nextIsCapital = false
                return DecodeResult.Unknown // consume — wait for next byte
            }

            // In number context: same dot patterns as letters but map to digits
            if (inNumberContext) {
                val letter = dotPatternToLetter[masked]
                val digit = letter?.let { letterToDigit[it] }
                return if (digit != null) {
                    DecodeResult.Character(digit)
                } else {
                    inNumberContext = false // end of number run
                    decodeLetter(masked)
                }
            }

            // Punctuation
            val punct = dotPatternToPunct[masked]
            if (punct != null) {
                nextIsCapital = false
                return DecodeResult.Character(punct)
            }

            // Letter
            return decodeLetter(masked)
        }

        private fun decodeLetter(masked: Int): DecodeResult {
            val letter = dotPatternToLetter[masked] ?: return DecodeResult.Unknown
            val result = if (nextIsCapital) letter.uppercaseChar() else letter
            nextIsCapital = false
            return DecodeResult.Character(result)
        }

        /** Reset context — call when the connection drops or a new session starts. */
        fun reset() {
            nextIsCapital = false
            inNumberContext = false
        }
    }
}
