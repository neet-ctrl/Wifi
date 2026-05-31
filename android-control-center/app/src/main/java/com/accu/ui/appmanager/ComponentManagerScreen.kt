package com.accu.ui.appmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.data.db.entities.BlockedComponentEntity
import com.accu.ui.components.ACCTopBar
import com.accu.ui.components.EmptyState
import com.accu.ui.privacy.PrivacyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentManagerScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ACCTopBar(title = "Component Manager", onBack = onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Stats
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Blocked", "${state.blockedComponents.size}", Modifier.weight(1f))
                StatCard("Trackers", "${state.trackerCount}", Modifier.weight(1f))
            }

            // Filter tabs
            var selectedType by remember { mutableStateOf("all") }
            val types = listOf("all", "activity", "service", "receiver", "provider")
            ScrollableTabRow(selectedTabIndex = types.indexOf(selectedType)) {
                types.forEach { type ->
                    Tab(selected = selectedType == type, onClick = { selectedType = type }, text = { Text(type.replaceFirstChar { it.uppercase() }) })
                }
            }

            val filtered = if (selectedType == "all") state.blockedComponents
                           else state.blockedComponents.filter { it.componentType == selectedType }

            if (filtered.isEmpty()) {
                EmptyState(Icons.Default.CheckCircle, "No blocked components", "Disable app components from App Detail screen")
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(filtered, key = { "${it.packageName}/${it.componentName}" }) { comp ->
                        BlockedComponentItem(comp = comp, onEnable = { viewModel.enableComponent(comp.packageName, comp.componentName) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedComponentItem(comp: BlockedComponentEntity, onEnable: () -> Unit) {
    ListItem(
        headlineContent = { Text(comp.componentName.substringAfterLast('.'), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(comp.packageName, style = MaterialTheme.typography.bodySmall)
                Text(comp.componentName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        leadingContent = {
            val icon = when (comp.componentType) {
                "activity" -> Icons.Default.Smartphone
                "service"  -> Icons.Default.Settings
                "receiver" -> Icons.Default.Notifications
                "provider" -> Icons.Default.Storage
                else -> Icons.Default.Block
            }
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = if (comp.isTracker) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(18.dp)) }
            }
        },
        trailingContent = {
            Row {
                if (comp.isTracker) Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text("Tracker", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(4.dp))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEnable) { Icon(Icons.Default.PlayArrow, "Enable") }
            }
        },
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
