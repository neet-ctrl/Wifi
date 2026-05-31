package com.accu.ui.appmanager

import androidx.compose.animation.AnimatedVisibility
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

data class DisabledApp(
    val name: String,
    val pkg: String,
    val disabledVia: DisabledVia,
    val sizeBytes: Long = 0L,
    val isSystem: Boolean = false,
    val disabledTimestamp: Long = System.currentTimeMillis(),
)

enum class DisabledVia(val label: String) {
    SYSTEM_SETTINGS("System Settings"),
    CANTA_BLOCKER("ACCU/Blocker"),
    ADB("ADB"),
    ROOT("Root"),
    UNKNOWN("Unknown"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InureDisabledAppsScreen(onBack: () -> Unit = {}) {
    var apps by remember {
        mutableStateOf(listOf(
            DisabledApp("Google Play Movies", "com.google.android.videos", DisabledVia.SYSTEM_SETTINGS, 14_000_000, false),
            DisabledApp("Carrier Services", "com.google.android.ims", DisabledVia.CANTA_BLOCKER, 8_200_000, true),
            DisabledApp("Device Health Services", "com.google.android.apps.turbo", DisabledVia.CANTA_BLOCKER, 6_100_000, true),
            DisabledApp("Android Auto", "com.google.android.projection.gearhead", DisabledVia.SYSTEM_SETTINGS, 45_000_000, true),
            DisabledApp("Face Unlock", "com.android.facelock", DisabledVia.ADB, 3_100_000, true),
            DisabledApp("Digital Wellbeing", "com.google.android.apps.wellbeing", DisabledVia.SYSTEM_SETTINGS, 28_000_000, false),
            DisabledApp("Google Magazine", "com.google.android.apps.magazines", DisabledVia.CANTA_BLOCKER, 12_000_000, false),
            DisabledApp("Samsung Bixby", "com.samsung.android.bixby.agent", DisabledVia.ROOT, 55_000_000, true),
            DisabledApp("Game Space", "com.samsung.android.game.gamehome", DisabledVia.ADB, 18_000_000, true),
        ))
    }
    var search by remember { mutableStateOf("") }
    var filterVia by remember { mutableStateOf<DisabledVia?>(null) }
    var sortBy by remember { mutableStateOf("name") }
    var showSortMenu by remember { mutableStateOf(false) }
    var confirmEnableApp by remember { mutableStateOf<DisabledApp?>(null) }
    var snackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(snackbar) { snackbar?.let { snackbarHost.showSnackbar(it); snackbar = null } }

    val filtered = apps.filter { app ->
        (filterVia == null || app.disabledVia == filterVia) &&
        (search.isBlank() || app.name.contains(search, ignoreCase = true) || app.pkg.contains(search, ignoreCase = true))
    }.let { list ->
        when (sortBy) {
            "size" -> list.sortedByDescending { it.sizeBytes }
            "via" -> list.sortedBy { it.disabledVia.label }
            else -> list.sortedBy { it.name }
        }
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
        bytes >= 1_000 -> "${bytes / 1_000} KB"
        else -> "$bytes B"
    }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Disabled Apps (${apps.size})",
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.Sort, "Sort") }
                        DropdownMenu(showSortMenu, { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("By Name") }, leadingIcon = { if (sortBy == "name") Icon(Icons.Default.Check, null) }, onClick = { sortBy = "name"; showSortMenu = false })
                            DropdownMenuItem(text = { Text("By Size") }, leadingIcon = { if (sortBy == "size") Icon(Icons.Default.Check, null) }, onClick = { sortBy = "size"; showSortMenu = false })
                            DropdownMenuItem(text = { Text("By Source") }, leadingIcon = { if (sortBy == "via") Icon(Icons.Default.Check, null) }, onClick = { sortBy = "via"; showSortMenu = false })
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Info card
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("${apps.size} disabled apps found", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Re-enabling system apps may restore functionality and break debloat. Use with caution.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(0.8f))
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search…") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null) } },
                singleLine = true,
            )

            // Filter by source chips
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(selected = filterVia == null, onClick = { filterVia = null }, label = { Text("All") })
                }
                items(DisabledVia.entries) { via ->
                    val count = apps.count { it.disabledVia == via }
                    if (count > 0) {
                        FilterChip(
                            selected = filterVia == via,
                            onClick = { filterVia = if (filterVia == via) null else via },
                            label = { Text("${via.label} ($count)") },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No disabled apps match filter", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(filtered, key = { it.pkg }) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(app.pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (app.disabledVia) {
                                                DisabledVia.CANTA_BLOCKER -> MaterialTheme.colorScheme.primaryContainer
                                                DisabledVia.ROOT -> MaterialTheme.colorScheme.errorContainer
                                                DisabledVia.ADB -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        ) {
                                            Text(app.disabledVia.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                        if (app.sizeBytes > 0) Text(formatSize(app.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        if (app.isSystem) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                                Text("System", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { confirmEnableApp = app },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                ) {
                                    Text("Enable", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm enable dialog
    val toEnable = confirmEnableApp
    if (toEnable != null) {
        AlertDialog(
            onDismissRequest = { confirmEnableApp = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Enable ${toEnable.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will re-enable the app using ${if (toEnable.isSystem) "Shizuku (pm enable)" else "Android API"}.", style = MaterialTheme.typography.bodySmall)
                    if (toEnable.isSystem) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("System app — re-enabling may restore background services and data collection from this component.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        apps = apps.filter { it.pkg != toEnable.pkg }
                        confirmEnableApp = null
                        snackbar = "Enabled ${toEnable.name}"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("Enable") }
            },
            dismissButton = { TextButton(onClick = { confirmEnableApp = null }) { Text("Cancel") } },
        )
    }
}
