package com.accu.ui.appmanager

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.accu.ui.components.ACCTopBar
import com.accu.ui.components.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebloatScreen(
    onBack: () -> Unit,
    viewModel: AppManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onTabChange(AppTab.SYSTEM) }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() }
    }

    // Pre-built debloat categories
    val bloatCategories = remember {
        mapOf(
            "Google Bloat" to listOf("com.google.android.apps.tachyon", "com.google.android.videos", "com.google.android.music", "com.google.android.apps.magazines", "com.google.android.apps.books"),
            "Samsung Bloat" to listOf("com.samsung.android.game.gamehome", "com.samsung.android.bixby.agent", "com.samsung.android.weather", "com.samsung.android.dialer.samsungdialerplugin"),
            "MIUI Bloat" to listOf("com.miui.player", "com.miui.video", "com.miui.analytics", "com.miui.msa.global", "com.miui.global.packageinstaller"),
            "Carrier Bloat" to listOf("com.att.myatt", "com.verizon.messaging.vzmsgs", "com.tmobile.pr.mytmobile"),
            "Ads & Analytics" to listOf("com.facebook.system", "com.facebook.orca", "com.facebook.services", "com.facebook.katana"),
        )
    }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Debloat",
                onBack = onBack,
                actions = {
                    if (state.selectedApps.isNotEmpty()) {
                        IconButton(onClick = { viewModel.batchUninstall() }) { Icon(Icons.Default.Delete, "Remove Selected") }
                    }
                    IconButton(onClick = { viewModel.toggleMultiSelect() }) { Icon(if (state.isMultiSelect) Icons.Default.CheckCircle else Icons.Default.SelectAll, null) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Warning banner
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Removing system apps may break functionality. Proceed with caution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // Search
            var searchQuery by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.onSearchChange(it) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                label = { Text("Search system apps…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
            )

            if (state.isLoading) {
                LoadingScreen("Loading system apps…")
            } else {
                // Preset debloat lists
                var selectedCategory by remember { mutableStateOf("") }
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bloatCategories.keys.toList()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) "" else cat },
                            label = { Text(cat) },
                        )
                    }
                }

                val systemApps = state.apps.filter { it.isSystemApp }
                    .let { list ->
                        val catFilter = bloatCategories[selectedCategory]
                        if (catFilter != null) list.filter { it.packageName in catFilter }
                        else if (searchQuery.isNotBlank()) list.filter { it.appName.contains(searchQuery, true) || it.packageName.contains(searchQuery, true) }
                        else list
                    }

                Text(
                    "${systemApps.size} system apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                LazyColumn(Modifier.weight(1f)) {
                    items(systemApps, key = { it.packageName }) { app ->
                        DebloatAppItem(
                            app = app,
                            isSelected = app.packageName in state.selectedApps,
                            isMultiSelect = state.isMultiSelect,
                            isBloat = bloatCategories.values.flatten().contains(app.packageName),
                            onClick = {
                                if (state.isMultiSelect) viewModel.toggleAppSelection(app.packageName)
                            },
                            onRemove = { viewModel.uninstallForUser(app.packageName) },
                            onFreeze = { viewModel.freezeApp(app.packageName) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DebloatAppItem(
    app: AppUiModel,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    isBloat: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onFreeze: () -> Unit,
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (isBloat) Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text("Bloat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
        },
        supportingContent = { Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            if (isMultiSelect) Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            else AsyncImage(
                model = ImageRequest.Builder(context).data(try { context.packageManager.getApplicationIcon(app.packageName) } catch (_: Exception) { null }).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onFreeze) { Icon(Icons.Default.AcUnit, "Freeze", Modifier.size(20.dp)) }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove", Modifier.size(20.dp)) }
            }
        },
        modifier = if (isMultiSelect) Modifier.clickable(onClick = onClick) else Modifier,
        colors = if (isSelected) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f)) else ListItemDefaults.colors(),
    )
}
private fun Modifier.clip(shape: androidx.compose.ui.graphics.Shape) = this.then(Modifier)
private fun Modifier.clickable(onClick: () -> Unit) = this.then(androidx.compose.ui.Modifier.clickable(onClick = onClick))
