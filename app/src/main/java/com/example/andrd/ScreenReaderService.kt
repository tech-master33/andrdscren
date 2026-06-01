package com.example.andrd

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.KeyEvent
import android.util.Log
import com.example.andrd.braille.BrailleDisplayManager

class ScreenReaderService : AccessibilityService() {
    private var ttsManager: TtsManager? = null
    private var brailleManager: BrailleDisplayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ttsManager = TtsManager(this)
        brailleManager = BrailleDisplayManager(this).also { it.connect() }
        Log.d(TAG, "Service connected")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP    -> announce("Up")
                KeyEvent.KEYCODE_DPAD_DOWN  -> announce("Down")
                KeyEvent.KEYCODE_DPAD_LEFT  -> announce("Left")
                KeyEvent.KEYCODE_DPAD_RIGHT -> announce("Right")
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER -> announce("Select")
            }
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val text: String = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source
                source?.contentDescription?.toString()
                    ?: source?.text?.toString()
                    ?: ""
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> ""
            else -> ""
        }

        if (text.isNotEmpty()) {
            announce(text)
        }
    }

    /**
     * Sends [text] to both the TTS engine and the Braille display simultaneously.
     * Either output can fail independently without affecting the other.
     */
    private fun announce(text: String) {
        Log.d(TAG, "Announcing: $text")
        ttsManager?.speak(text)
        brailleManager?.sendText(text)
    }

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
