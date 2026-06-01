package com.accu.ui.audio

import android.app.Activity
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.audio.*
import com.accu.ui.components.ACCTopBar
import com.accu.ui.components.InfoTooltipIcon
import com.accu.ui.theme.AccentGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundMasterScreen(
    onBack: () -> Unit,
    viewModel: SoundMasterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let { snackbarHost.showSnackbar(it); viewModel.clearSnackbar() }
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data -> viewModel.onProjectionGranted(data, ctx) }
        }
    }

    var showAddAppSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf<SoundMasterEntry?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Sound Master",
                onBack = onBack,
                actions = {
                    InfoTooltipIcon(
                        title = "Sound Master",
                        description = "Per-app volume control with advanced EQ.\n\n" +
                            "• Control each app's volume independently (0–200%)\n" +
                            "• Stereo balance control per app\n" +
                            "• 3-band EQ: Lows / Mids / Highs\n" +
                            "• Route audio to any output device\n" +
                            "• Built-in presets (Flat, Bass, Vocal…)\n" +
                            "• Volume boost: >100% uses LoudnessEnhancer\n\n" +
                            "Uses Android AudioPlaybackCapture API via ACCU privilege. " +
                            "No Shizuku required."
                    )
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SoundMasterSort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = { viewModel.onSortChanged(sort); showSortMenu = false },
                                    leadingIcon = {
                                        if (state.sortBy == sort) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.loadInstalledApps()
                    showAddAppSheet = true
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add App") },
                containerColor = MaterialTheme.colorScheme.primary,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Service Control Card ──
            ServiceControlCard(
                isRunning = state.isServiceRunning,
                entryCount = state.entries.size,
                latencyMap = state.latencyMap,
                onStart = { activity ->
                    viewModel.grantPermissionsAndStart(activity) { pm ->
                        projectionLauncher.launch(pm.createScreenCaptureIntent())
                    }
                },
                onStop = { viewModel.stopService(ctx) },
                ctx = ctx,
            )

            // ── Search + Quick Actions ──
            SearchAndActionsBar(
                query = state.filterQuery,
                onQueryChange = viewModel::onFilterChanged,
                onMuteAll = { viewModel.muteAll() },
                onResetAllEq = { viewModel.resetAllEq() },
            )

            if (state.entries.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val filtered = viewModel.filteredEntries()
                    if (filtered.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                                Text("No apps match filter", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(filtered, key = { "${it.pkg}:${it.outputDeviceId}" }) { entry ->
                            SoundMasterEntryCard(
                                entry = entry,
                                devices = state.audioDevices,
                                isServiceRunning = state.isServiceRunning,
                                rms = state.rmsMap[entry.pkg] ?: 0f,
                                onVolumeChange = { viewModel.updateVolume(entry, it) },
                                onBalanceChange = { viewModel.updateBalance(entry, it) },
                                onEqChange = { band, v -> viewModel.updateEqBand(entry, band, v) },
                                onRemove = { viewModel.removeEntry(entry) },
                                onReset = { viewModel.resetEntry(entry) },
                                onPreset = { showPresetSheet = entry },
                                onToggleLock = { viewModel.toggleLocked(entry) },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Add App Bottom Sheet ──
    if (showAddAppSheet) {
        AddAppBottomSheet(
            apps = state.availableApps,
            devices = state.audioDevices,
            existingEntries = state.entries,
            isLoading = state.isLoadingApps,
            onAdd = { pkg, deviceId -> viewModel.addEntry(pkg, pkg.split(".").last(), deviceId) },
            onDismiss = { showAddAppSheet = false },
        )
    }

    // ── Preset Sheet ──
    showPresetSheet?.let { entry ->
        PresetBottomSheet(
            entry = entry,
            presets = state.presets,
            onApply = { preset -> viewModel.applyPreset(entry, preset); showPresetSheet = null },
            onDismiss = { showPresetSheet = null },
        )
    }

    // ── Settings Sheet ──
    if (showSettingsSheet) {
        SoundMasterSettingsSheet(
            showNotification = state.showNotification,
            showOnVolumeChange = state.showOnVolumeChange,
            autoHide = state.autoHide,
            onSave = { n, v, a -> viewModel.saveSettings(n, v, a); showSettingsSheet = false },
            onDismiss = { showSettingsSheet = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ServiceControlCard(
    isRunning: Boolean,
    entryCount: Int,
    latencyMap: Map<String, Float>,
    onStart: (Activity) -> Unit,
    onStop: () -> Unit,
    ctx: Context,
) {
    val activity = ctx as? Activity
    val statusColor = if (isRunning) AccentGreen else MaterialTheme.colorScheme.error
    val avgLatency = if (latencyMap.isNotEmpty()) latencyMap.values.average().toInt() else 0

    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(0.1f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isRunning) "Sound Master Active" else "Sound Master Stopped",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isRunning) "Controlling $entryCount app${if (entryCount != 1) "s" else ""} · Avg latency: ${avgLatency}ms"
                    else "Add apps below, then press Start",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(
                onClick = { if (isRunning) onStop() else activity?.let { onStart(it) } },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = statusColor.copy(0.2f),
                    contentColor = statusColor,
                ),
            ) {
                Icon(if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isRunning) "Stop" else "Start")
            }
        }
    }
}

@Composable
private fun SearchAndActionsBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMuteAll: () -> Unit,
    onResetAllEq: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search apps…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onMuteAll,
                label = { Text("Mute All", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.VolumeOff, null, Modifier.size(14.dp)) },
            )
            AssistChip(
                onClick = onResetAllEq,
                label = { Text("Reset EQ", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Refresh, null, Modifier.size(14.dp)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundMasterEntryCard(
    entry: SoundMasterEntry,
    devices: List<AudioDeviceInfo?>,
    isServiceRunning: Boolean,
    rms: Float,
    onVolumeChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onEqChange: (Int, Float) -> Unit,
    onRemove: () -> Unit,
    onReset: () -> Unit,
    onPreset: () -> Unit,
    onToggleLock: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val appLabel = remember(entry.pkg) { entry.pkg.split(".").last().replaceFirstChar { it.uppercase() } }
    val isBoost = entry.volume > 100f
    val deviceName = devices.find { it?.id == entry.outputDeviceId }.displayName()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (expanded) 6.dp else 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Header ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(appLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (isBoost) {
                            Spacer(Modifier.width(6.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                Text("BOOST", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (entry.locked) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isServiceRunning && rms > 0) {
                            Spacer(Modifier.width(6.dp))
                            RmsIndicator(rms = rms)
                        }
                    }
                    Text(
                        entry.pkg,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "→ $deviceName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPreset, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Tune, "Presets", Modifier.size(18.dp))
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        Modifier.size(18.dp),
                    )
                }
            }

            // ── Volume Slider ──
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = entry.volume,
                    onValueChange = { if (!entry.locked) onVolumeChange(it) },
                    valueRange = 0f..200f,
                    modifier = Modifier.weight(1f),
                    enabled = !entry.locked,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${"%.0f".format(entry.volume)}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBoost) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(44.dp),
                )
            }

            // ── Expanded Controls ──
            AnimatedVisibility(visible = expanded) {
                Column {
                    Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // Balance
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("L", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(14.dp))
                        Slider(
                            value = entry.balance,
                            onValueChange = { if (!entry.locked) onBalanceChange(it) },
                            valueRange = -100f..100f,
                            modifier = Modifier.weight(1f),
                            enabled = !entry.locked,
                        )
                        Text("R", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    Text("Balance: ${"%.0f".format(entry.balance)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                    Spacer(Modifier.height(8.dp))

                    // EQ Bands
                    Text("Equalizer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Lows" to entry.eqLow, "Mids" to entry.eqMid, "Highs" to entry.eqHigh).forEachIndexed { band, (label, value) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${if (value >= 50) "+" else ""}${"%.0f".format(value - 50)}dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (value != 50f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Slider(
                                    value = value,
                                    onValueChange = { if (!entry.locked) onEqChange(band, it) },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(80.dp).width(32.dp),
                                    enabled = !entry.locked,
                                )
                                Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))

                    // Action Row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onToggleLock, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                            Icon(if (entry.locked) Icons.Default.LockOpen else Icons.Default.Lock, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (entry.locked) "Unlock" else "Lock", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onRemove,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Default.Delete, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RmsIndicator(rms: Float) {
    val animatedRms by animateFloatAsState(
        targetValue = (rms / 32768f).coerceIn(0f, 1f),
        animationSpec = tween(200),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(4) { i ->
            val active = animatedRms > (i * 0.25f)
            Box(
                Modifier
                    .width(2.dp)
                    .height((6 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (active) AccentGreen else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppBottomSheet(
    apps: List<Pair<String, String>>,
    devices: List<AudioDeviceInfo?>,
    existingEntries: List<SoundMasterEntry>,
    isLoading: Boolean,
    onAdd: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPkg by remember { mutableStateOf<String?>(null) }
    var selectedDevice by remember { mutableStateOf<AudioDeviceInfo?>(null) }
    var search by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Add App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = apps.filter { (pkg, name) ->
                    search.isBlank() || name.contains(search, true) || pkg.contains(search, true)
                }
                LazyColumn(Modifier.fillMaxWidth().height(300.dp)) {
                    items(filtered) { (pkg, name) ->
                        val alreadyAdded = existingEntries.any { it.pkg == pkg }
                        ListItem(
                            headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(pkg, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingContent = {
                                if (alreadyAdded) {
                                    Text("Added", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                                } else if (selectedPkg == pkg) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable(enabled = !alreadyAdded) { selectedPkg = if (selectedPkg == pkg) null else pkg },
                            colors = if (selectedPkg == pkg) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f)) else ListItemDefaults.colors(),
                        )
                    }
                }
            }
            if (devices.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text("Output Device", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices) { device ->
                        FilterChip(
                            selected = selectedDevice == device,
                            onClick = { selectedDevice = device },
                            label = { Text(device.displayName(), fontSize = 11.sp) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    selectedPkg?.let { pkg ->
                        onAdd(pkg, selectedDevice?.id ?: -1)
                        onDismiss()
                    }
                },
                enabled = selectedPkg != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add to Sound Master")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetBottomSheet(
    entry: SoundMasterEntry,
    presets: List<SoundMasterPreset>,
    onApply: (SoundMasterPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Presets — ${entry.pkg.split(".").last()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            presets.forEach { preset ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    onClick = { onApply(preset) },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(preset.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Vol: ${"%.0f".format(preset.volume)}% · Lows: ${"%.0f".format(preset.eqLow - 50)}dB · Mids: ${"%.0f".format(preset.eqMid - 50)}dB · Highs: ${"%.0f".format(preset.eqHigh - 50)}dB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundMasterSettingsSheet(
    showNotification: Boolean,
    showOnVolumeChange: Boolean,
    autoHide: Boolean,
    onSave: (Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var notif by remember { mutableStateOf(showNotification) }
    var volChange by remember { mutableStateOf(showOnVolumeChange) }
    var hide by remember { mutableStateOf(autoHide) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Sound Master Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            SettingsSwitch("Show notification", "Display a persistent notification while active", notif) { notif = it }
            SettingsSwitch("Show on volume change", "Open Sound Master when system volume changes", volChange) { volChange = it }
            SettingsSwitch("Auto-hide after 3s", "Automatically close the controls after inactivity", hide) { hide = it }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onSave(notif, volChange, hide) }, Modifier.fillMaxWidth()) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VolumeUp, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
            Spacer(Modifier.height(16.dp))
            Text("No apps added yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap ＋ Add App to control per-app volume, EQ, and output routing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
