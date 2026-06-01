package com.accu.ui.audio

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.audio.MixedAudioAppState
import com.accu.audio.MixedAudioFocus
import com.accu.ui.components.ACCTopBar
import com.accu.ui.components.InfoTooltipIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixedAudioScreen(
    onBack: () -> Unit,
    viewModel: MixedAudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let { snackbarHost.showSnackbar(it); viewModel.clearSnackbar() }
    }

    var showPresetMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<MixedAudioAppState?>(null) }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Mixed Audio",
                onBack = onBack,
                actions = {
                    InfoTooltipIcon(
                        title = "Mixed Audio",
                        description = "Per-app audio focus and mute control.\n\n" +
                            "• Mute: completely silence any app (PLAY_AUDIO deny)\n" +
                            "• MixedAudio Ignored: apps don't pause others when gaining focus\n" +
                            "• MixedAudio Denied: app can never steal audio focus\n\n" +
                            "Batch mode: select multiple apps and apply changes at once.\n" +
                            "Quick presets: Music, Podcast, Silent, Restore All.\n\n" +
                            "All changes use ADB appops via ACCU — no Shizuku required."
                    )
                    if (!state.isBatchMode) {
                        Box {
                            IconButton(onClick = { showPresetMenu = true }) {
                                Icon(Icons.Default.AutoAwesome, "Presets")
                            }
                            DropdownMenu(expanded = showPresetMenu, onDismissRequest = { showPresetMenu = false }) {
                                Text("  Quick Presets", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                listOf(
                                    Triple(MixedAudioPreset.MUSIC_MODE,  "Music Mode",   Icons.Default.MusicNote),
                                    Triple(MixedAudioPreset.PODCAST_MODE,"Podcast Mode", Icons.Default.Podcasts),
                                    Triple(MixedAudioPreset.SILENT_MODE, "Silent Mode",  Icons.Default.VolumeOff),
                                    Triple(MixedAudioPreset.RESTORE_ALL, "Restore All",  Icons.Default.RestartAlt),
                                ).forEach { (preset, label, icon) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        leadingIcon = { Icon(icon, null) },
                                        onClick = { viewModel.applyQuickPreset(preset); showPresetMenu = false },
                                    )
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                Text("  Sort By", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MixedAudioSort.values().forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = { viewModel.onSortChanged(s); showSortMenu = false },
                                        leadingIcon = { if (state.sortBy == s) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showRestoreDialog = true }) {
                            Icon(Icons.Default.RestartAlt, "Restore")
                        }
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleBatchMode()
                    }) {
                        Icon(
                            if (state.isBatchMode) Icons.Default.Close else Icons.Default.Checklist,
                            if (state.isBatchMode) "Exit Batch" else "Batch Mode",
                        )
                    }
                    IconButton(onClick = { viewModel.loadApps() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Warning Banner ──
            MixedAudioWarning()

            // ── Stats Row ──
            val modified = state.apps.count { it.muted || it.focus != MixedAudioFocus.ALLOWED }
            StatsRow(total = state.apps.size, modified = modified)

            // ── Search + Filter ──
            SearchAndFilterBar(
                query = state.filterQuery,
                filterMode = state.filterMode,
                onQueryChange = viewModel::onFilterChanged,
                onFilterChange = viewModel::onFilterModeChanged,
            )

            // ── Batch Action Bar ──
            AnimatedVisibility(visible = state.isBatchMode) {
                BatchActionBar(
                    selectedCount = state.selectedPkgs.size,
                    onMute = { viewModel.batchMute() },
                    onUnmute = { viewModel.batchUnmute() },
                    onIgnoreFocus = { viewModel.batchSetFocus(MixedAudioFocus.IGNORED) },
                    onDenyFocus = { viewModel.batchSetFocus(MixedAudioFocus.DENIED) },
                    onAllowFocus = { viewModel.batchSetFocus(MixedAudioFocus.ALLOWED) },
                )
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading apps…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val filtered = viewModel.filteredApps()
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("No apps found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filtered, key = { it.pkg }) { app ->
                            MixedAudioAppRow(
                                app = app,
                                isBatchMode = state.isBatchMode,
                                isSelected = app.pkg in state.selectedPkgs,
                                onSelect = { viewModel.toggleSelection(app.pkg) },
                                onClick = { actionTarget = app },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Action Bottom Sheet ──
    actionTarget?.let { target ->
        AppActionSheet(
            app = target,
            onMute = { viewModel.muteApp(target.pkg); actionTarget = null },
            onUnmute = { viewModel.unmuteApp(target.pkg); actionTarget = null },
            onSetFocus = { f -> viewModel.setFocus(target.pkg, f); actionTarget = null },
            onDismiss = { actionTarget = null },
        )
    }

    // ── Restore Dialog ──
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Audio Settings") },
            text = { Text("Choose what to restore for all apps:") },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            },
            icon = { Icon(Icons.Default.RestartAlt, null) },
        )
        // Custom layout with multiple actions
        val options = listOf(
            "Unmute all apps" to { viewModel.unmuteAll() },
            "Reset all audio focus" to { viewModel.resetAllFocus() },
            "Full restore" to { viewModel.applyQuickPreset(MixedAudioPreset.RESTORE_ALL) },
        )
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Audio Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (label, action) ->
                        Button(onClick = { action(); showRestoreDialog = false }, Modifier.fillMaxWidth()) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MixedAudioWarning() {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(0.6f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(
                "Don't unmute apps currently controlled by Sound Master",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun StatsRow(total: Int, modified: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatChip("$total apps", Icons.Default.Apps, MaterialTheme.colorScheme.primaryContainer)
        if (modified > 0) {
            StatChip("$modified modified", Icons.Default.Edit, MaterialTheme.colorScheme.secondaryContainer)
        }
    }
}

@Composable
private fun StatChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    query: String,
    filterMode: MixedAudioFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (MixedAudioFilter) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search apps or packages…") },
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
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(MixedAudioFilter.values().toList()) { filter ->
                FilterChip(
                    selected = filterMode == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                    leadingIcon = if (filterMode == filter) ({ Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }) else null,
                )
            }
        }
    }
}

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onIgnoreFocus: () -> Unit,
    onDenyFocus: () -> Unit,
    onAllowFocus: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "$selectedCount app${if (selectedCount != 1) "s" else ""} selected",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilledTonalButton(onClick = onMute, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.VolumeOff, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mute", fontSize = 12.sp)
                    }
                }
                item {
                    FilledTonalButton(onClick = onUnmute, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.VolumeUp, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Unmute", fontSize = 12.sp)
                    }
                }
                item {
                    FilledTonalButton(onClick = onIgnoreFocus, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("Ignore Focus", fontSize = 12.sp)
                    }
                }
                item {
                    FilledTonalButton(onClick = onDenyFocus, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("Deny Focus", fontSize = 12.sp)
                    }
                }
                item {
                    FilledTonalButton(onClick = onAllowFocus, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("Allow Focus", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MixedAudioAppRow(
    app: MixedAudioAppState,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
) {
    val isModified = app.muted || app.focus != MixedAudioFocus.ALLOWED
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isBatchMode) onSelect else onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                isModified -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isBatchMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isModified) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (app.muted) {
                        Spacer(Modifier.width(6.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("MUTED", fontSize = 9.sp)
                        }
                    }
                }
                Text(
                    app.pkg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            FocusChip(focus = app.focus)
            if (!isBatchMode) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FocusChip(focus: MixedAudioFocus) {
    val (label, color) = when (focus) {
        MixedAudioFocus.ALLOWED  -> "Allow"  to MaterialTheme.colorScheme.primaryContainer
        MixedAudioFocus.IGNORED  -> "Ignore" to MaterialTheme.colorScheme.secondaryContainer
        MixedAudioFocus.DENIED   -> "Deny"   to MaterialTheme.colorScheme.errorContainer
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppActionSheet(
    app: MixedAudioAppState,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onSetFocus: (MixedAudioFocus) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(app.pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(containerColor = if (app.muted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer) {
                    Text(if (app.muted) "MUTED" else "UNMUTED", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
                FocusChip(focus = app.focus)
            }
            Spacer(Modifier.height(16.dp))

            // Mute/Unmute
            Text("Volume Access", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMute,
                    modifier = Modifier.weight(1f),
                    enabled = !app.muted,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.VolumeOff, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mute")
                }
                OutlinedButton(
                    onClick = onUnmute,
                    modifier = Modifier.weight(1f),
                    enabled = app.muted,
                ) {
                    Icon(Icons.Default.VolumeUp, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Unmute")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Audio Focus
            Text("Audio Focus", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "• Allow: default — app can pause other audio\n" +
                    "• Ignore: app plays alongside others (MixedAudio)\n" +
                    "• Deny: app can never gain audio focus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            listOf(MixedAudioFocus.ALLOWED, MixedAudioFocus.IGNORED, MixedAudioFocus.DENIED).forEach { focus ->
                val (label, desc) = when (focus) {
                    MixedAudioFocus.ALLOWED -> "Allow Focus" to "Normal audio focus behavior"
                    MixedAudioFocus.IGNORED -> "Ignore Focus (MixedAudio)" to "App plays without pausing others"
                    MixedAudioFocus.DENIED  -> "Deny Focus (Force)" to "App can never get audio focus"
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    onClick = { onSetFocus(focus) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (app.focus == focus) MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(label, fontWeight = if (app.focus == focus) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (app.focus == focus) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
