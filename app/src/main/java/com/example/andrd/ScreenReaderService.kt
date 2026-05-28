package com.example.andrd

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.KeyEvent
import android.util.Log

class ScreenReaderService : AccessibilityService() {
    private var ttsManager: TtsManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ttsManager = TtsManager(this)
        Log.d("ScreenReaderService", "Service connected")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        if (action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> ttsManager?.speak("Up")
                KeyEvent.KEYCODE_DPAD_DOWN -> ttsManager?.speak("Down")
                KeyEvent.KEYCODE_DPAD_LEFT -> ttsManager?.speak("Left")
                KeyEvent.KEYCODE_DPAD_RIGHT -> ttsManager?.speak("Right")
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> ttsManager?.speak("Select")
            }
        }
        // Return false to let the system handle the actual navigation
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        var textToSpeak = ""

        when (eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source
                textToSpeak = source?.contentDescription?.toString()
                    ?: source?.text?.toString()
                    ?: ""
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Window changed, could be a new screen or menu
                // We'll be conservative here to avoid constant chatter
            }
        }

        if (textToSpeak.isNotEmpty()) {
            Log.d("ScreenReaderService", "Speaking: $textToSpeak")
            ttsManager?.speak(textToSpeak)
        }
    }

    override fun onInterrupt() {
        Log.d("ScreenReaderService", "Service interrupted")
    }

    override fun onDestroy() {
        ttsManager?.shutdown()
        super.onDestroy()
    }
}
