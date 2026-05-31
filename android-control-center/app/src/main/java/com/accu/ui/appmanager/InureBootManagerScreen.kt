package com.accu.ui.appmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accu.ui.components.ACCTopBar

data class BootReceiver(
    val appName: String,
    val packageName: String,
    val receiverClass: String,
    val isEnabled: Boolean,
    val isSystem: Boolean = false,
    val disabledByAcf: Boolean = false,
    val action: String = "BOOT_COMPLETED",
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InureBootManagerScreen(onBack: () -> Unit = {}) {
    var apps by remember {
        mutableStateOf(listOf(
            BootReceiver("WhatsApp", "com.whatsapp", "com.whatsapp.receiver.StartupReceiver", true),
            BootReceiver("Spotify", "com.spotify.music", "com.spotify.receiver.BootReceiver", true),
            BootReceiver("Google Play Services", "com.google.android.gms", "com.google.android.gms.BootCompleteReceiver", true, true),
            BootReceiver("Tasker", "net.dinglisch.android.taskerm", "net.dinglisch.android.taskerm.TaskerBootReceiver", true),
            BootReceiver("Syncthing", "com.nutomic.syncthingandroid", "com.nutomic.syncthingandroid.receiver.BootReceiver", false, disabledByAcf = true),
            BootReceiver("Automate", "com.llamalab.automate", "com.llamalab.automate.BootReceiver", false, disabledByAcf = true),
            BootReceiver("ACCU", "com.accu.controlcenter", "com.accu.receiver.BootReceiver", true, true),
            BootReceiver("ACCU Service", "com.accu.controlcenter", "com.accu.receivers.BootReceiver", true),
            BootReceiver("Gmail", "com.google.android.gm", "com.google.android.gm.receiver.BootReceiver", true),
            BootReceiver("AlarmManager", "com.android.deskclock", "com.android.deskclock.AlarmInitReceiver", true, true),
            BootReceiver("Telegram", "org.telegram.messenger", "org.telegram.messenger.NotificationsService${"$"}BootReceiver", true),
            BootReceiver("Signal", "org.thoughtcrime.securesms", "org.thoughtcrime.securesms.BootReceiver", true),
            BootReceiver("Maps", "com.google.android.apps.maps", "com.google.android.apps.maps.BootReceiver", false, disabledByAcf = true),
        ))
    }
    var search by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    var filterEnabled by remember { mutableStateOf<Boolean?>(null) } // null = all, true = enabled, false = disabled
    var selectedPkgs by remember { mutableStateOf(setOf<String>()) }
    var snackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    val isSelecting = selectedPkgs.isNotEmpty()

    LaunchedEffect(snackbar) { snackbar?.let { snackbarHost.showSnackbar(it); snackbar = null } }

    val filtered = apps.filter { app ->
        (showSystem || !app.isSystem) &&
        (filterEnabled == null || app.isEnabled == filterEnabled) &&
        (search.isBlank() || app.appName.contains(search, ignoreCase = true) || app.packageName.contains(search, ignoreCase = true))
    }.sortedWith(compareByDescending<BootReceiver> { !it.isEnabled }.thenBy { it.appName })

    val enabledCount = apps.count { it.isEnabled }
    val acfDisabledCount = apps.count { it.disabledByAcf }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = if (isSelecting) "${selectedPkgs.size} selected" else "Boot Manager",
                onBack = {
                    if (isSelecting) selectedPkgs = emptySet()
                    else onBack()
                },
                actions = {
                    if (isSelecting) {
                        IconButton(onClick = {
                            apps = apps.map { if (it.packageName in selectedPkgs) it.copy(isEnabled = true, disabledByAcf = false) else it }
                            snackbar = "Enabled ${selectedPkgs.size} receivers"
                            selectedPkgs = emptySet()
                        }) { Icon(Icons.Default.PlayArrow, "Enable selected") }
                        IconButton(onClick = {
                            apps = apps.map { if (it.packageName in selectedPkgs) it.copy(isEnabled = false, disabledByAcf = true) else it }
                            snackbar = "Disabled ${selectedPkgs.size} receivers"
                            selectedPkgs = emptySet()
                        }) { Icon(Icons.Default.Stop, "Disable selected") }
                        IconButton(onClick = { selectedPkgs = emptySet() }) { Icon(Icons.Default.Close, "Clear selection") }
                    } else {
                        IconButton(onClick = { showInfo = true }) { Icon(Icons.Default.Info, null) }
                        IconButton(onClick = { showSystem = !showSystem }) {
                            Icon(
                                if (showSystem) Icons.Default.Android else Icons.Default.PhoneAndroid,
                                null,
                                tint = if (!showSystem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Stats bar
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    BootStatItem("${apps.size}", "Total")
                    BootStatItem("$enabledCount", "Auto-start")
                    BootStatItem("${apps.size - enabledCount}", "Disabled")
                    BootStatItem("$acfDisabledCount", "By ACCU")
                }
            }

            // Search
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search receivers…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null) } },
                singleLine = true,
            )

            // Filter chips
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = filterEnabled == null, onClick = { filterEnabled = null }, label = { Text("All") })
                FilterChip(selected = filterEnabled == true, onClick = { filterEnabled = if (filterEnabled == true) null else true }, label = { Text("Enabled ($enabledCount)") })
                FilterChip(selected = filterEnabled == false, onClick = { filterEnabled = if (filterEnabled == false) null else false }, label = { Text("Disabled (${apps.size - enabledCount})") })
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No receivers match filter", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isSelected = app.packageName in selectedPkgs
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .combinedClickable(
                                    onClick = { if (isSelecting) selectedPkgs = if (isSelected) selectedPkgs - app.packageName else selectedPkgs + app.packageName },
                                    onLongClick = { selectedPkgs = selectedPkgs + app.packageName },
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    !app.isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceContainer
                                }
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isSelecting) {
                                    Checkbox(checked = isSelected, onCheckedChange = { ch -> selectedPkgs = if (ch) selectedPkgs + app.packageName else selectedPkgs - app.packageName })
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(
                                    if (app.isSystem) Icons.Default.Android else Icons.Default.Apps,
                                    null,
                                    tint = when {
                                        !app.isEnabled -> MaterialTheme.colorScheme.outline
                                        app.isSystem -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (app.disabledByAcf) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                                Text("ACCU", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        app.receiverClass.substringAfterLast('.'),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Switch(
                                    checked = app.isEnabled,
                                    onCheckedChange = { en ->
                                        apps = apps.map { if (it.packageName == app.packageName) it.copy(isEnabled = en, disabledByAcf = !en) else it }
                                        snackbar = "${app.appName} boot receiver ${if (en) "enabled" else "disabled"}"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            icon = { Icon(Icons.Default.PowerSettingsNew, null) },
            title = { Text("Boot Manager") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Controls which apps receive the BOOT_COMPLETED broadcast. Disabling removes the app's ability to auto-start on device boot.", style = MaterialTheme.typography.bodySmall)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How it works:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("• Uses pm command via ACCU to disable/enable broadcast receivers\n• Affects only BOOT_COMPLETED, QUICKBOOT_POWERON actions\n• Does NOT disable the app itself — just prevents auto-start\n• Long-press any app to enter selection mode for batch operations", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Note: Re-enabling a boot receiver takes effect on next device restart.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = { showInfo = false }) { Text("Got it") } },
        )
    }
}

@Composable
private fun BootStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
    }
}

