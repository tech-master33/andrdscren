package com.example.andrd.braille

/**
 * Converts plain text to Grade 1 English Braille Unicode characters.
 *
 * Grade 1 (uncontracted) Braille maps each letter, digit, and common
 * punctuation mark to a single Braille cell. No contractions are used.
 * Each Braille cell is represented as a Unicode character in the range
 * U+2800–U+28FF.
 *
 * Output is a string of Braille Unicode characters ready to send to a
 * Braille display over Bluetooth.
 */
object BrailleTranslator {

    // Grade 1 English Braille — letters a–z
    private val letterMap: Map<Char, Char> = mapOf(
        'a' to '\u2801', // ⠁ dot 1
        'b' to '\u2803', // ⠃ dots 1 2
        'c' to '\u2809', // ⠉ dots 1 4
        'd' to '\u2819', // ⠙ dots 1 4 5
        'e' to '\u2811', // ⠑ dots 1 5
        'f' to '\u280B', // ⠋ dots 1 2 4
        'g' to '\u281B', // ⠛ dots 1 2 4 5
        'h' to '\u2813', // ⠓ dots 1 2 5
        'i' to '\u280A', // ⠊ dots 2 4
        'j' to '\u281A', // ⠚ dots 2 4 5
        'k' to '\u2805', // ⠅ dots 1 3
        'l' to '\u2807', // ⠇ dots 1 2 3
        'm' to '\u280D', // ⠍ dots 1 3 4
        'n' to '\u281D', // ⠝ dots 1 3 4 5
        'o' to '\u2815', // ⠕ dots 1 3 5
        'p' to '\u280F', // ⠏ dots 1 2 3 4
        'q' to '\u281F', // ⠟ dots 1 2 3 4 5
        'r' to '\u2817', // ⠗ dots 1 2 3 5
        's' to '\u280E', // ⠎ dots 2 3 4
        't' to '\u281E', // ⠞ dots 2 3 4 5
        'u' to '\u2825', // ⠥ dots 1 3 6
        'v' to '\u2827', // ⠧ dots 1 2 3 6
        'w' to '\u283A', // ⠺ dots 2 4 5 6
        'x' to '\u282D', // ⠭ dots 1 3 4 6
        'y' to '\u283D', // ⠽ dots 1 3 4 5 6
        'z' to '\u2835'  // ⠵ dots 1 3 5 6
    )

    // Digits — preceded by the number indicator ⠼ (U+283C)
    // Digits 1–9 use the same cells as letters a–i; 0 uses the j cell
    private val digitMap: Map<Char, Char> = mapOf(
        '1' to '\u2801', '2' to '\u2803', '3' to '\u2809',
        '4' to '\u2819', '5' to '\u2811', '6' to '\u280B',
        '7' to '\u281B', '8' to '\u2813', '9' to '\u280A',
        '0' to '\u281A'
    )

    private const val NUMBER_INDICATOR = '\u283C'  // ⠼
    private const val CAPITAL_INDICATOR = '\u2820' // ⠠ dot 6
    private const val BRAILLE_SPACE = '\u2800'     // ⠀ empty cell

    // Common punctuation
    private val punctuationMap: Map<Char, Char> = mapOf(
        ',' to '\u2802',  // ⠂ dot 2
        ';' to '\u2806',  // ⠆ dots 2 3
        ':' to '\u2812',  // ⠒ dots 2 5
        '.' to '\u2832',  // ⠲ dots 2 5 6
        '!' to '\u2816',  // ⠖ dots 2 3 5
        '?' to '\u2826',  // ⠦ dots 2 3 5 6
        '"' to '\u2826',  // treat as question mark cell (approximate)
        '\'' to '\u2804', // ⠄ dot 3
        '-' to '\u2824',  // ⠤ dots 3 6
        '(' to '\u2836',  // ⠶ dots 2 3 5 6 (opening)
        ')' to '\u2836',  // ⠶ dots 2 3 5 6 (closing)
        '/' to '\u280C',  // ⠌ dots 3 4
        '@' to '\u281C',  // ⠜ dots 3 4 5
        '#' to '\u283C',  // ⠼ dots 3 4 5 6 (number indicator reused)
        '&' to '\u282F',  // ⠯ dots 1 2 3 4 6
        '*' to '\u2814'   // ⠔ dots 3 5
    )

    /**
     * Translates [text] into a Grade 1 Braille Unicode string.
     *
     * Rules applied:
     * - Uppercase letters are preceded by the capital indicator ⠠
     * - Digit runs are preceded by the number indicator ⠼ (once per run)
     * - Spaces become the empty Braille cell ⠀
     * - Newlines become a space cell followed by a line-feed (for displays that support it)
     * - Unknown characters are replaced with a space cell
     *
     * The result can be sent byte-by-byte (UTF-8) over Bluetooth to any
     * display that accepts Unicode Braille input.
     */
    fun translate(text: String): String {
        val result = StringBuilder()
        var inNumberContext = false

        for (ch in text) {
            when {
                ch == ' ' || ch == '\t' -> {
                    result.append(BRAILLE_SPACE)
                    inNumberContext = false
                }
                ch == '\n' -> {
                    result.append(BRAILLE_SPACE)
                    result.append('\n')
                    inNumberContext = false
                }
                ch.isDigit() -> {
                    if (!inNumberContext) {
                        result.append(NUMBER_INDICATOR)
                        inNumberContext = true
                    }
                    result.append(digitMap[ch] ?: BRAILLE_SPACE)
                }
                ch.isLetter() -> {
                    inNumberContext = false
                    if (ch.isUpperCase()) {
                        result.append(CAPITAL_INDICATOR)
                    }
                    result.append(letterMap[ch.lowercaseChar()] ?: BRAILLE_SPACE)
                }
                punctuationMap.containsKey(ch) -> {
                    inNumberContext = false
                    result.append(punctuationMap[ch]!!)
                }
                else -> {
                    inNumberContext = false
                    result.append(BRAILLE_SPACE)
                }
            }
        }

        return result.toString()
    }

    /**
     * Returns the maximum number of cells [text] will occupy on a Braille display.
     * Useful for deciding whether to scroll or truncate.
     */
    fun cellCount(text: String): Int = translate(text).length
}
