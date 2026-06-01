package com.example.andrd

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.KeyEvent
import android.util.Log
import com.example.andrd.braille.BrailleDisplayManager
import com.example.andrd.braille.BrailleInputDecoder
import com.example.andrd.braille.BrailleInputHandler

class ScreenReaderService : AccessibilityService() {

    private var ttsManager: TtsManager? = null
    private var brailleManager: BrailleDisplayManager? = null
    private var brailleInput: BrailleInputHandler? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ttsManager = TtsManager(this)

        // BrailleInputHandler needs a reference to this service so it can
        // perform accessibility actions (click, navigate, set text).
        val inputHandler = BrailleInputHandler(this)
        brailleInput = inputHandler

        // Wire the display manager: every decoded input byte is routed
        // to the input handler immediately.
        brailleManager = BrailleDisplayManager(this) { result ->
            handleBrailleInput(result, inputHandler)
        }.also { it.connect() }

        Log.d(TAG, "Service connected")
    }

    // ── Accessibility events ──────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val text: String = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Clear the Braille type buffer when focus moves to a new view
                brailleInput?.clearBuffer()
                val source = event.source
                source?.contentDescription?.toString()
                    ?: source?.text?.toString()
                    ?: ""
            }
            else -> ""
        }
        if (text.isNotEmpty()) announce(text)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP     -> announce("Up")
                KeyEvent.KEYCODE_DPAD_DOWN   -> announce("Down")
                KeyEvent.KEYCODE_DPAD_LEFT   -> announce("Left")
                KeyEvent.KEYCODE_DPAD_RIGHT  -> announce("Right")
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER -> announce("Select")
            }
        }
        return false
    }

    // ── Braille input routing ─────────────────────────────────────────────

    /**
     * Routes a decoded Braille input event to the appropriate handler action.
     * This is called from the BrailleDisplayManager's background read loop,
     * so it must be thread-safe. AccessibilityNodeInfo API calls are safe
     * from any thread.
     */
    private fun handleBrailleInput(
        result: BrailleInputDecoder.DecodeResult,
        handler: BrailleInputHandler
    ) {
        when (result) {
            is BrailleInputDecoder.DecodeResult.Character -> {
                Log.d(TAG, "Braille char: '${result.char}'")
                handler.onCharacter(result.char)
                // Speak what was typed so the user gets audio confirmation
                ttsManager?.speak(result.char.toString())
            }
            is BrailleInputDecoder.DecodeResult.Backspace -> {
                Log.d(TAG, "Braille backspace")
                handler.onBackspace()
                ttsManager?.speak("delete")
            }
            is BrailleInputDecoder.DecodeResult.Command -> {
                Log.d(TAG, "Braille command: 0x${result.code.toString(16)}")
                handler.onCommand(result.code)
            }
            is BrailleInputDecoder.DecodeResult.Unknown -> { /* ignore */ }
        }
    }

    // ── Shared announce ───────────────────────────────────────────────────

    /**
     * Sends [text] to both the TTS engine and the Braille display.
     * Either can fail independently without affecting the other.
     */
    private fun announce(text: String) {
        Log.d(TAG, "Announcing: $text")
        ttsManager?.speak(text)
        brailleManager?.sendText(text)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        brailleManager?.disconnect()
        ttsManager?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ScreenReaderService"
    }
}
