package com.example.andrd.braille

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Handles decoded Braille input events from [BrailleInputDecoder.Decoder]
 * and translates them into actions inside the [AccessibilityService].
 *
 * Two modes:
 *
 *   TYPING mode — the focused view is an editable text field.
 *     Characters append to the field.
 *     Backspace deletes the last character.
 *     The activate command (routing key) confirms/submits.
 *
 *   NAVIGATION mode — the focused view is anything else.
 *     Characters are announced aloud and ignored for text input
 *     (the user is probably trying to navigate, not type).
 *     Command keys trigger AccessibilityService global actions:
 *       pan right / scroll down → GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT is avoided;
 *                                  instead we simulate swipe-right (next element)
 *       pan left  / scroll up   → previous element
 *       home                    → GLOBAL_ACTION_HOME
 *       back                    → GLOBAL_ACTION_BACK
 *       activate                → click the currently focused view
 *
 * The service calls [onCharacter] and [onCommand] for every decoded event.
 */
class BrailleInputHandler(private val service: AccessibilityService) {

    private val typeBuffer = StringBuilder()

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Called when the user types a character on the Braille display keyboard.
     */
    fun onCharacter(char: Char) {
        val focused = focusedNode()

        if (focused != null && focused.isEditable) {
            // Typing mode: append to the text field
            typeBuffer.append(char)
            setNodeText(focused, typeBuffer.toString())
            Log.d(TAG, "Braille input → typed '$char', buffer='$typeBuffer'")
        } else {
            // Navigation mode: just speak the character
            Log.d(TAG, "Braille input char '$char' — not in editable field, announcing")
        }

        focused?.recycle()
    }

    /**
     * Called when the user presses a navigation/command key on the display.
     */
    fun onCommand(code: Int) {
        Log.d(TAG, "Braille command: 0x${code.toString(16)}")
        when (code) {
            BrailleInputDecoder.CMD_PAN_RIGHT,
            BrailleInputDecoder.CMD_SCROLL_DOWN -> focusNext()

            BrailleInputDecoder.CMD_PAN_LEFT,
            BrailleInputDecoder.CMD_SCROLL_UP   -> focusPrevious()

            BrailleInputDecoder.CMD_HOME -> {
                typeBuffer.clear()
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }

            BrailleInputDecoder.CMD_BACK -> {
                if (typeBuffer.isNotEmpty()) {
                    // If typing, back = clear the buffer
                    typeBuffer.clear()
                    focusedNode()?.let { node ->
                        setNodeText(node, "")
                        node.recycle()
                    }
                } else {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                }
            }

            BrailleInputDecoder.CMD_ACTIVATE -> {
                typeBuffer.clear()
                focusedNode()?.let { node ->
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                }
            }
        }
    }

    /**
     * Called when the user presses the backspace key combination on the display.
     */
    fun onBackspace() {
        val focused = focusedNode()
        if (focused != null && focused.isEditable && typeBuffer.isNotEmpty()) {
            typeBuffer.deleteCharAt(typeBuffer.length - 1)
            setNodeText(focused, typeBuffer.toString())
            Log.d(TAG, "Braille backspace, buffer='$typeBuffer'")
        }
        focused?.recycle()
    }

    /** Clear the internal type buffer — call when focus moves to a new field. */
    fun clearBuffer() {
        typeBuffer.clear()
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun focusedNode(): AccessibilityNodeInfo? =
        service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)

    private fun setNodeText(node: AccessibilityNodeInfo, text: String) {
        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /**
     * Move accessibility focus to the next element (same as swiping right
     * on a touch screen with a screen reader active).
     */
    private fun focusNext() {
        val root = service.rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (focused != null) {
            focused.performAction(
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS
                    .let { AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY }
            )
            val args = Bundle()
            args.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
            )
            args.putBoolean(
                AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                false
            )
            focused.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, args
            )
            focused.recycle()
        } else {
            // No focus yet — focus the first element
            root.findAccessibilityNodeInfosByText("")
                .firstOrNull()
                ?.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        }
        root.recycle()
    }

    /**
     * Move accessibility focus to the previous element (same as swiping left).
     */
    private fun focusPrevious() {
        val root = service.rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (focused != null) {
            val args = Bundle()
            args.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
            )
            args.putBoolean(
                AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                false
            )
            focused.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, args
            )
            focused.recycle()
        }
        root.recycle()
    }

    companion object {
        private const val TAG = "BrailleInputHandler"
    }
}
