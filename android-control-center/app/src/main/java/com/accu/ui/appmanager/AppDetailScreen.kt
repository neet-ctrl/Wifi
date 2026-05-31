package com.accu.ui.appmanager

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.accu.ui.components.ACCTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(packageName) { viewModel.load(packageName) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(topBar = {
        ACCTopBar(
            title = state.appName.ifBlank { packageName },
            onBack = onBack,
            actions = {
                IconButton(onClick = { viewModel.extractApk() }) { Icon(Icons.Default.Download, "Extract APK") }
                IconButton(onClick = { viewModel.forceStop() }) { Icon(Icons.Default.Stop, "Force Stop") }
            },
        )
    }) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
                // Header
                item {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(try { context.packageManager.getApplicationIcon(packageName) } catch (_: Exception) { null })
                                .crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(state.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("v${state.versionName} (${state.versionCode})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                // Action buttons
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilledTonalButton(onClick = { viewModel.toggleFreeze() }) { Icon(Icons.Default.AcUnit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(if (state.isFrozen) "Unfreeze" else "Freeze") } }
                        item { FilledTonalButton(onClick = { viewModel.toggleHide() }) { Icon(Icons.Default.VisibilityOff, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Hide") } }
                        item { OutlinedButton(onClick = { viewModel.clearData() }) { Text("Clear Data") } }
                        item { OutlinedButton(onClick = { viewModel.uninstall() }) { Text("Uninstall") } }
                    }
                }

                // Info cards
                item {
                    DetailSection("Package Information") {
                        DetailRow("Package", packageName)
                        DetailRow("Version", "v${state.versionName} (${state.versionCode})")
                        DetailRow("Min SDK", "API ${state.minSdk}")
                        DetailRow("Target SDK", "API ${state.targetSdk}")
                        DetailRow("APK Size", if (state.apkSize > 0) "${"%.2f".format(state.apkSize / 1_000_000.0)} MB" else "Unknown")
                        DetailRow("Install Date", if (state.installTime > 0) dateFormatter.format(Date(state.installTime)) else "Unknown")
                        DetailRow("Last Update", if (state.lastUpdate > 0) dateFormatter.format(Date(state.lastUpdate)) else "Unknown")
                        DetailRow("APK Path", state.sourceDir)
                        DetailRow("Data Dir", state.dataDir)
                    }
                }

                // Permissions (Inure)
                if (state.permissions.isNotEmpty()) {
                    item {
                        DetailSection("Permissions (${state.permissions.size})") {
                            state.permissions.forEach { perm ->
                                PermissionItem(perm, onRevoke = { viewModel.revokePermission(perm.name) }, onGrant = { viewModel.grantPermission(perm.name) })
                            }
                        }
                    }
                }

                // Activities (Inure)
                if (state.activities.isNotEmpty()) {
                    item {
                        DetailSection("Activities (${state.activities.size})") {
                            state.activities.forEach { act ->
                                ComponentItem(act.name, "Activity",
                                    isEnabled = act.isEnabled,
                                    onToggle = { viewModel.toggleComponent(act.name, "activity") },
                                    onLaunch = { viewModel.launchActivity(act.name) },
                                )
                            }
                        }
                    }
                }

                // Services (Inure)
                if (state.services.isNotEmpty()) {
                    item {
                        DetailSection("Services (${state.services.size})") {
                            state.services.forEach { svc ->
                                ComponentItem(svc.name, "Service",
                                    isEnabled = svc.isEnabled,
                                    onToggle = { viewModel.toggleComponent(svc.name, "service") },
                                    onLaunch = null,
                                )
                            }
                        }
                    }
                }

                // Receivers (Inure + Blocker)
                if (state.receivers.isNotEmpty()) {
                    item {
                        DetailSection("Receivers (${state.receivers.size})") {
                            state.receivers.forEach { recv ->
                                ComponentItem(recv.name, "Receiver",
                                    isEnabled = recv.isEnabled,
                                    onToggle = { viewModel.toggleComponent(recv.name, "receiver") },
                                    onLaunch = null,
                                )
                            }
                        }
                    }
                }

                // Providers (Inure)
                if (state.providers.isNotEmpty()) {
                    item {
                        DetailSection("Providers (${state.providers.size})") {
                            state.providers.forEach { prov ->
                                ComponentItem(prov.name, "Provider",
                                    isEnabled = prov.isEnabled,
                                    onToggle = { viewModel.toggleComponent(prov.name, "provider") },
                                    onLaunch = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PermissionItem(perm: PermissionUiModel, onRevoke: () -> Unit, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (perm.isGranted) Icons.Default.Check else Icons.Default.Block,
            null,
            tint = if (perm.isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(perm.name.substringAfterLast('.'), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (perm.isProtected) Text("Protected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        if (!perm.isProtected) {
            TextButton(onClick = if (perm.isGranted) onRevoke else onGrant, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text(if (perm.isGranted) "Revoke" else "Grant", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ComponentItem(name: String, type: String, isEnabled: Boolean, onToggle: () -> Unit, onLaunch: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name.substringAfterLast('.'), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onLaunch != null) IconButton(onClick = onLaunch, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)) }
        Switch(checked = isEnabled, onCheckedChange = { onToggle() }, modifier = Modifier.scale(0.75f))
    }
}

private fun Modifier.scale(factor: Float) = this.then(Modifier.size((48 * factor).dp, (24 * factor).dp))
