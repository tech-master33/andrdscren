package com.example.andrd.braille

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manages the Bluetooth SPP connection to a Braille display.
 *
 * OUTPUT: translates text to Grade 1 Braille Unicode and writes it to the
 *         display's output stream via [sendText].
 *
 * INPUT:  reads bytes from the display's input stream in a background
 *         coroutine and passes each decoded event to [onInput].
 *         The caller (ScreenReaderService) provides [onInput] so it can
 *         act on characters and commands without this class needing a
 *         reference to the service.
 */
class BrailleDisplayManager(
    private val context: Context,
    /** Called on every decoded input event from the display's keyboard. */
    private val onInput: ((BrailleInputDecoder.DecodeResult) -> Unit)? = null
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = BraillePreferences(context)
    private val decoder = BrailleInputDecoder.Decoder()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    @Volatile private var connected = false

    // ── Device selection ──────────────────────────────────────────────────

    fun pairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices.toList()
    }

    fun selectDevice(device: BluetoothDevice) {
        prefs.deviceAddress = device.address
        prefs.deviceName = device.name
        prefs.enabled = true
        connect()
    }

    // ── Connection lifecycle ──────────────────────────────────────────────

    fun connect() {
        if (!prefs.enabled) return
        val address = prefs.deviceAddress ?: return
        if (connected) return
        scope.launch { connectToAddress(address) }
    }

    private suspend fun connectToAddress(address: String) = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                Log.w(TAG, "Bluetooth not available")
                return@withContext
            }
            val device = adapter.getRemoteDevice(address)
            adapter.cancelDiscovery()

            Log.i(TAG, "Connecting to ${device.name} ($address)")
            val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            newSocket.connect()

            socket = newSocket
            outputStream = newSocket.outputStream
            inputStream  = newSocket.inputStream
            connected = true
            decoder.reset()

            Log.i(TAG, "Connected to ${device.name}")
            sendRaw(BrailleTranslator.translate("BAOSP ready") + "\r")

            // Start the read loop now that we have a live input stream
            startInputLoop(newSocket.inputStream)

        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            connected = false
            socket = null; outputStream = null; inputStream = null
        }
    }

    /**
     * Runs continuously in the background, reading one byte at a time from
     * the display's input stream and forwarding decoded results to [onInput].
     *
     * The loop exits cleanly when the socket is closed or the coroutine
     * scope is cancelled (i.e. when [disconnect] is called).
     */
    private fun startInputLoop(stream: InputStream) {
        scope.launch(Dispatchers.IO) {
            Log.i(TAG, "Braille input loop started")
            val buf = ByteArray(1)
            try {
                while (isActive && connected) {
                    val bytesRead = stream.read(buf)
                    if (bytesRead < 0) break          // stream closed by display
                    val result = decoder.decode(buf[0].toInt())
                    if (result !is BrailleInputDecoder.DecodeResult.Unknown) {
                        Log.d(TAG, "Braille input: $result")
                        onInput?.invoke(result)
                    }
                }
            } catch (e: IOException) {
                if (connected) Log.w(TAG, "Input stream closed: ${e.message}")
            } finally {
                Log.i(TAG, "Braille input loop ended")
                connected = false
            }
        }
    }

    fun disconnect() {
        connected = false
        try { outputStream?.close() } catch (_: IOException) {}
        try { inputStream?.close()  } catch (_: IOException) {}
        try { socket?.close()       } catch (_: IOException) {}
        outputStream = null; inputStream = null; socket = null
        decoder.reset()
    }

    // ── Output ────────────────────────────────────────────────────────────

    fun sendText(text: String) {
        if (!connected) return
        scope.launch {
            val braille = BrailleTranslator.translate(text)
            val width = prefs.displayWidth
            val line = if (braille.length > width) braille.take(width) else braille
            sendRaw(line + "\r")
        }
    }

    private fun sendRaw(raw: String) {
        try {
            outputStream?.write(raw.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Send failed: ${e.message}")
            connected = false
            socket = null; outputStream = null; inputStream = null
        }
    }

    // ── State queries ─────────────────────────────────────────────────────

    fun isConnected(): Boolean = connected
    fun savedDeviceName(): String? = prefs.deviceName

    companion object {
        private const val TAG = "BrailleDisplayManager"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
