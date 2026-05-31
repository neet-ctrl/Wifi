package com.accu.ui.shizuku

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.ui.components.*
import com.accu.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuCenterScreen(
    onBack: () -> Unit,
    viewModel: ShizukuViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPairingDialog by remember { mutableStateOf(false) }
    var pairingCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Shizuku Center",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) { Icon(Icons.Default.ClearAll, "Clear Logs") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Refresh, "Refresh") }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Status Card
            item {
                ShizukuStatusCard(
                    state = state,
                    onRequestPermission = viewModel::requestPermission,
                    onStartWithAdb = viewModel::startWithAdb,
                    onStartWithRoot = viewModel::startWithRoot,
                )
            }

            // Wireless ADB Section
            item {
                WirelessAdbCard(
                    state = state,
                    onEnable = viewModel::enableWirelessAdb,
                    onDisable = viewModel::disableWirelessAdb,
                    onStartPairing = { showPairingDialog = true },
                )
            }

            // Device info
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Device Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        DeviceInfoRow("IP Address", state.deviceIp.ifEmpty { "Unknown" })
                        DeviceInfoRow("ADB Port", if (state.wirelessAdbPort > 0) "${state.wirelessAdbPort}" else "Not set")
                        DeviceInfoRow("Shizuku Version", if (state.version > 0) "v${state.version}" else "N/A")
                        DeviceInfoRow("Shizuku UID", if (state.uid > 0) "${state.uid}" else "N/A")
                        DeviceInfoRow("Root Access", if (state.isRootAvailable) "Available" else "Not available")
                    }
                }
            }

            // Quick Commands
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Quick Commands", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        val cmds = listOf(
                            "Check permission" to "dumpsys package com.accu.controlcenter | grep permission",
                            "List Shizuku clients" to "dumpsys activity services moe.shizuku",
                            "ADB connect local" to "adb connect 127.0.0.1:5555",
                            "Get Android version" to "getprop ro.build.version.release",
                            "Battery info" to "dumpsys battery",
                        )
                        cmds.forEach { (label, cmd) ->
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Default.Terminal, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Logs
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Diagnostics Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        if (state.logs.isEmpty()) {
                            Text("No logs yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.logs.reversed().take(50).forEach { entry ->
                                val color = when (entry.level) {
                                    LogLevel.SUCCESS -> AccentGreen
                                    LogLevel.ERROR   -> AccentRed
                                    LogLevel.WARNING -> AccentOrange
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                val prefix = when (entry.level) {
                                    LogLevel.SUCCESS -> "✓"
                                    LogLevel.ERROR   -> "✗"
                                    LogLevel.WARNING -> "⚠"
                                    else -> "ℹ"
                                }
                                Text(
                                    "$prefix ${entry.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }

            // Setup guide
            item {
                SetupGuideCard()
            }
        }
    }

    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = { showPairingDialog = false },
            title = { Text("ADB Wireless Pairing") },
            text = {
                Column {
                    Text("Enable developer options → Wireless debugging → Pair device with pairing code, then enter the 6-digit code below.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it.take(6) },
                        label = { Text("Pairing Code") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPairingDialog = false; viewModel.startPairing(pairingCode) }) { Text("Pair") }
            },
            dismissButton = { TextButton(onClick = { showPairingDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShizukuStatusCard(
    state: ShizukuUiState,
    onRequestPermission: () -> Unit,
    onStartWithAdb: () -> Unit,
    onStartWithRoot: () -> Unit,
) {
    val statusColor = when {
        state.isAvailable && state.isGranted -> AccentGreen
        state.isAvailable -> AccentOrange
        state.isRootAvailable -> AccentCyan
        else -> AccentRed
    }
    val statusText = when {
        state.isAvailable && state.isGranted -> "Shizuku Running & Authorized"
        state.isAvailable -> "Shizuku Running — Permission Required"
        state.isRootAvailable -> "Root Mode Available"
        state.isInstalled -> "Shizuku Installed — Not Running"
        else -> "Shizuku Not Installed"
    }

    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(0.1f)),
        border = BorderStroke(1.dp, statusColor.copy(0.4f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.isAvailable && state.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    null, tint = statusColor, modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Shizuku Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!state.isAvailable) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onStartWithAdb, Modifier.weight(1f)) {
                        Icon(Icons.Default.Usb, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("via ADB")
                    }
                    if (state.isRootAvailable) {
                        Button(onClick = onStartWithRoot, Modifier.weight(1f)) {
                            Icon(Icons.Default.AdminPanelSettings, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("via Root")
                        }
                    }
                }
            } else if (!state.isGranted) {
                Button(onClick = onRequestPermission, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Key, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun WirelessAdbCard(
    state: ShizukuUiState,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onStartPairing: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Wireless ADB", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(checked = state.isWirelessAdbEnabled, onCheckedChange = { if (it) onEnable() else onDisable() })
            }
            if (state.deviceIp.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "adb connect ${state.deviceIp}:${state.wirelessAdbPort}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onStartPairing, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCode, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Pair New Device")
            }
        }
    }
}

@Composable
private fun SetupGuideCard() {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Setup Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val steps = listOf(
                "Install Shizuku from Play Store or F-Droid",
                "Enable Developer Options on your device",
                "Enable Wireless Debugging (Android 11+) or USB Debugging",
                "Open Shizuku app and start the service",
                "Grant ACC permission when prompted",
                "Enjoy elevated access without root!",
            )
            steps.forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(20.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("${i+1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(step, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
