package com.accu.ui.appmanager

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
import com.accu.ui.components.EmptyState
import com.accu.ui.components.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreezeAppsScreen(
    onBack: () -> Unit,
    viewModel: AppManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() }
    }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "Freeze Apps",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Add, "Add app to freeze list") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {},
                icon = { Icon(Icons.Default.AcUnit, null) },
                text = { Text("Freeze All") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Freeze method info card
            Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Freeze Methods", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Disable" to "Disable app via pm. Requires Shizuku or root.",
                        "Suspend" to "Suspend via ActivityManager. Shows greyed icon.",
                        "Hide" to "Hides app from launcher. Like uninstall without data loss.",
                    ).forEach { (method, desc) ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("• $method: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (state.frozenApps.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AcUnit,
                    title = "No Frozen Apps",
                    subtitle = "Go to App Manager and freeze apps to see them here.",
                )
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(state.frozenApps, key = { it.packageName }) { frozen ->
                        ListItem(
                            headlineContent = { Text(frozen.appName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Column {
                                    Text(frozen.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Method: ${frozen.freezeMethod} · Auto-freeze on screen off: ${if (frozen.autoFreezeOnScreenOff) "Yes" else "No"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(try { context.packageManager.getApplicationIcon(frozen.packageName) } catch (_: Exception) { null }).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(frozen.freezeMethod, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp))
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(onClick = { viewModel.unfreezeApp(frozen.packageName) }) {
                                        Icon(Icons.Default.PlayArrow, "Unfreeze", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
