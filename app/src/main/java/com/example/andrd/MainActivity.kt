package com.example.andrd

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.andrd.braille.BrailleDisplayManager
import com.example.andrd.braille.BraillePreferences

class MainActivity : ComponentActivity() {

    private lateinit var brailleManager: BrailleDisplayManager
    private lateinit var braillePrefs: BraillePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brailleManager = BrailleDisplayManager(this)
        braillePrefs = BraillePreferences(this)

        setContent {
            ScreenReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SetupScreen(
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        brailleManager = brailleManager,
                        braillePrefs = braillePrefs
                    )
                }
            }
        }
    }
}

@Composable
fun SetupScreen(
    onOpenSettings: () -> Unit,
    brailleManager: BrailleDisplayManager,
    braillePrefs: BraillePreferences
) {
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var showDevicePicker by remember { mutableStateOf(false) }
    var savedDeviceName by remember { mutableStateOf(braillePrefs.deviceName) }
    var brailleEnabled by remember { mutableStateOf(braillePrefs.enabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Screen reader section ──────────────────────────────────────────
        item {
            Text(
                text = "BAOSP Screen Reader",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        item {
            Text(
                text = "Enable the screen reader in Accessibility Settings. " +
                       "It will speak the content of each element as you navigate.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        item {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings")
            }
        }

        // ── Divider ───────────────────────────────────────────────────────
        item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

        // ── Braille display section ────────────────────────────────────────
        item {
            Text(
                text = "Braille Display",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        item {
            Text(
                text = "Connect a Bluetooth Braille display to receive screen content " +
                       "as raised dots in real time. Pair the display in Bluetooth " +
                       "Settings first, then select it here.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Show currently saved device
        if (savedDeviceName != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Selected display",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = savedDeviceName ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (brailleEnabled) "Braille output on" else "Braille output off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (brailleEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Toggle on/off without forgetting the device
                    OutlinedButton(
                        onClick = {
                            braillePrefs.enabled = !braillePrefs.enabled
                            brailleEnabled = braillePrefs.enabled
                            if (braillePrefs.enabled) brailleManager.connect()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (brailleEnabled) "Turn off" else "Turn on")
                    }
                    // Forget this device and choose another
                    OutlinedButton(
                        onClick = {
                            brailleManager.disconnect()
                            braillePrefs.clearDevice()
                            savedDeviceName = null
                            brailleEnabled = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Forget display")
                    }
                }
            }
        }

        // Button to scan paired devices
        item {
            Button(
                onClick = {
                    pairedDevices = brailleManager.pairedDevices()
                    showDevicePicker = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (savedDeviceName == null)
                        "Choose Braille display"
                    else
                        "Change Braille display"
                )
            }
        }

        // Device list (shown after tapping the button above)
        if (showDevicePicker) {
            if (pairedDevices.isEmpty()) {
                item {
                    Text(
                        text = "No paired Bluetooth devices found. " +
                               "Go to Settings → Connected devices → Bluetooth and " +
                               "pair your Braille display first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                item {
                    Text(
                        text = "Select your Braille display from the list below:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(pairedDevices) { device ->
                    OutlinedButton(
                        onClick = {
                            brailleManager.selectDevice(device)
                            savedDeviceName = device.name
                            brailleEnabled = true
                            showDevicePicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = device.name ?: "Unknown device",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── Display width setting ──────────────────────────────────────────
        if (brailleEnabled) {
            item { Divider(modifier = Modifier.padding(vertical = 4.dp)) }
            item {
                Text(
                    text = "Display width: ${braillePrefs.displayWidth} cells",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                Text(
                    text = "Set this to match how many Braille cells your display has. " +
                           "Common sizes: 20, 32, 40, 80.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 32, 40, 80).forEach { width ->
                        OutlinedButton(
                            onClick = { braillePrefs.displayWidth = width }
                        ) {
                            Text("$width")
                        }
                    }
                }
            }
        }

        // ── Help note ─────────────────────────────────────────────────────
        item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
        item {
            Text(
                text = "If your Braille display is not in the list, make sure it is " +
                       "paired (not just nearby) in Bluetooth settings. " +
                       "Most displays use the Serial Port Profile (SPP). " +
                       "BLE-only displays are not yet supported.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScreenReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(),
        content = content
    )
}
