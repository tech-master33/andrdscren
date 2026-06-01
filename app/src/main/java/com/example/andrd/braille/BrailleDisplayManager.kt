package com.example.andrd.braille

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Manages the Bluetooth connection to a Braille display and sends text to it.
 *
 * Connection uses the Serial Port Profile (SPP / RFCOMM), which is the most
 * widely supported protocol across commercial Braille displays. The display
 * receives UTF-8–encoded Unicode Braille characters and renders them as
 * raised dots.
 *
 * Usage:
 *   val manager = BrailleDisplayManager(context)
 *   manager.connect()          // called once when the service starts
 *   manager.sendText("Hello")  // called whenever focus changes
 *   manager.disconnect()       // called when the service stops
 */
class BrailleDisplayManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = BraillePreferences(context)

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @Volatile
    private var connected = false

    /**
     * Returns a list of paired Bluetooth devices for the user to choose from.
     * The UI should call this and show the list so the user can pick their display.
     */
    fun pairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices.toList()
    }

    /**
     * Saves [device] as the Braille display and connects to it immediately.
     */
    fun selectDevice(device: BluetoothDevice) {
        prefs.deviceAddress = device.address
        prefs.deviceName = device.name
        prefs.enabled = true
        connect()
    }

    /**
     * Connects to the saved Braille display device in the background.
     * Does nothing if no device has been saved or if already connected.
     */
    fun connect() {
        if (!prefs.enabled) return
        val address = prefs.deviceAddress ?: return
        if (connected) return

        scope.launch {
            connectToAddress(address)
        }
    }

    private suspend fun connectToAddress(address: String) = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                Log.w(TAG, "Bluetooth not available or not enabled")
                return@withContext
            }

            val device = adapter.getRemoteDevice(address)
            adapter.cancelDiscovery()

            Log.i(TAG, "Connecting to Braille display: ${device.name} ($address)")

            val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            newSocket.connect()

            socket = newSocket
            outputStream = newSocket.outputStream
            connected = true

            Log.i(TAG, "Connected to Braille display: ${device.name}")

            // Send a welcome message so the user knows the connection worked
            sendRaw(BrailleTranslator.translate("BAOSP ready"))
            sendRaw("\n")

        } catch (e: IOException) {
            Log.e(TAG, "Failed to connect to Braille display: ${e.message}")
            connected = false
            socket = null
            outputStream = null
        }
    }

    /**
     * Translates [text] to Grade 1 Braille Unicode and sends it to the display.
     * Silently does nothing if not connected.
     *
     * Text is truncated to [prefs.displayWidth] cells if longer, so the display
     * does not receive more characters than it can show at once.
     * A carriage-return character is appended to move the display cursor to
     * the start of the line after rendering.
     */
    fun sendText(text: String) {
        if (!connected) return
        scope.launch {
            val braille = BrailleTranslator.translate(text)
            val width = prefs.displayWidth
            val truncated = if (braille.length > width) braille.take(width) else braille
            sendRaw(truncated)
            sendRaw("\r") // carriage return — most displays reset cursor on \r
        }
    }

    /**
     * Sends [raw] as UTF-8 bytes directly to the display output stream.
     */
    private fun sendRaw(raw: String) {
        try {
            outputStream?.write(raw.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Lost connection to Braille display: ${e.message}")
            connected = false
            socket = null
            outputStream = null
        }
    }

    /**
     * Closes the Bluetooth connection cleanly.
     * Call this when the AccessibilityService is destroyed.
     */
    fun disconnect() {
        connected = false
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Error while disconnecting: ${e.message}")
        } finally {
            outputStream = null
            socket = null
        }
    }

    /** Whether a Braille display is currently connected. */
    fun isConnected(): Boolean = connected

    /** Name of the currently selected device, or null if none saved. */
    fun savedDeviceName(): String? = prefs.deviceName

    companion object {
        private const val TAG = "BrailleDisplayManager"

        // Standard Bluetooth Serial Port Profile UUID — works with virtually
        // all commercial Braille displays that support Bluetooth Classic.
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
