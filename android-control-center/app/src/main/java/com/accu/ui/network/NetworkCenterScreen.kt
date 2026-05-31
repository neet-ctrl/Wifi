package com.accu.ui.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.ui.components.*
import com.accu.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkCenterScreen(
    onBack: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() }
    }

    Scaffold(
        topBar = { ACCTopBar(title = "Network Center", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Quick toggle tiles (from BetterInternetTiles)
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Quick Toggles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Rootless toggles via Shizuku — no root required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NetworkTile("Wi-Fi", Icons.Default.Wifi, state.wifiEnabled, AccentCyan, { viewModel.toggleWifi() }, Modifier.weight(1f))
                            NetworkTile("Mobile Data", Icons.Default.CellTower, state.mobileDataEnabled, AccentGreen, { viewModel.toggleMobileData() }, Modifier.weight(1f))
                            NetworkTile("Hotspot", Icons.Default.Wifi, state.hotspotEnabled, AccentOrange, { viewModel.toggleHotspot() }, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NetworkTile("Bluetooth", Icons.Default.Bluetooth, state.bluetoothEnabled, AccentPurple, { viewModel.toggleBluetooth() }, Modifier.weight(1f))
                            NetworkTile("NFC", Icons.Default.Nfc, state.nfcEnabled, AccentYellow, { viewModel.toggleNfc() }, Modifier.weight(1f))
                            NetworkTile("Airplane", Icons.Default.AirplanemodeActive, state.airplaneModeEnabled, AccentRed, { viewModel.toggleAirplaneMode() }, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Wi-Fi info
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Wi-Fi Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        DetailRow2("SSID", state.wifiSsid.ifBlank { "Not connected" })
                        DetailRow2("IP Address", state.wifiIp.ifBlank { "—" })
                        DetailRow2("MAC Address", state.wifiMac.ifBlank { "—" })
                        DetailRow2("Signal Strength", if (state.wifiSignal > 0) "${state.wifiSignal} dBm" else "—")
                        DetailRow2("Link Speed", if (state.wifiLinkSpeed > 0) "${state.wifiLinkSpeed} Mbps" else "—")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.forgetWifi() }) { Text("Forget") }
                            OutlinedButton(onClick = { viewModel.scanNetworks() }) { Text("Scan Networks") }
                        }
                    }
                }
            }

            // Quick Shell Commands (from aShellYou)
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Network Commands", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        val cmds = listOf(
                            "Check connectivity" to "ping -c 4 8.8.8.8",
                            "DNS lookup" to "nslookup google.com",
                            "Show network interfaces" to "ip addr show",
                            "Show routing table" to "ip route show",
                            "Wi-Fi scan" to "cmd wifi start-scan",
                            "ADB TCP mode" to "setprop service.adb.tcp.port 5555",
                            "Disable ADB TCP" to "setprop service.adb.tcp.port -1",
                            "Show data usage" to "dumpsys netstats",
                        )
                        cmds.forEach { (label, cmd) ->
                            OutlinedButton(
                                onClick = { viewModel.executeCmd(cmd) },
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Default.Terminal, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // QS Tile Setup (from BetterInternetTiles)
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Quick Settings Tiles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Add ACC network tiles to your Quick Settings panel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        listOf("Wi-Fi Toggle Tile", "Mobile Data Tile", "Hotspot Tile").forEach { tile ->
                            FeatureRow(
                                title = tile,
                                subtitle = "Tap to add to Quick Settings",
                                leadingIcon = { Icon(Icons.Default.AddCircle, null) },
                                onClick = { viewModel.addQsTile(tile) },
                            )
                        }
                    }
                }
            }

            // Command output
            if (state.cmdOutput.isNotBlank()) {
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp))) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Output", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearOutput() }) { Icon(Icons.Default.Close, "Close", Modifier.size(16.dp)) }
                            }
                            Text(state.cmdOutput, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (enabled) color.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DetailRow2(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
