package com.example.andrd.braille

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the user's chosen Braille display Bluetooth MAC address
 * and user-facing settings across app restarts.
 */
class BraillePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /** MAC address of the paired Bluetooth device chosen as the Braille display. */
    var deviceAddress: String?
        get() = prefs.getString(KEY_DEVICE_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ADDRESS, value).apply()

    /** Human-readable name of the chosen device, shown in the UI. */
    var deviceName: String?
        get() = prefs.getString(KEY_DEVICE_NAME, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_NAME, value).apply()

    /** Number of Braille cells on the display (default 40 — most common). */
    var displayWidth: Int
        get() = prefs.getInt(KEY_DISPLAY_WIDTH, DEFAULT_DISPLAY_WIDTH)
        set(value) = prefs.edit().putInt(KEY_DISPLAY_WIDTH, value).apply()

    /** Whether Braille output is enabled at all. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Clear the stored device so the user must choose again. */
    fun clearDevice() {
        prefs.edit()
            .remove(KEY_DEVICE_ADDRESS)
            .remove(KEY_DEVICE_NAME)
            .apply()
    }

    companion object {
        private const val PREF_FILE = "braille_display_prefs"
        private const val KEY_DEVICE_ADDRESS = "device_address"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_DISPLAY_WIDTH = "display_width"
        private const val KEY_ENABLED = "enabled"
        const val DEFAULT_DISPLAY_WIDTH = 40
    }
}
