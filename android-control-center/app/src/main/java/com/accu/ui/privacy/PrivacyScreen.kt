package com.accu.ui.privacy

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accu.ui.components.ACCTopBar
import com.accu.ui.components.EmptyState
import com.accu.ui.components.LoadingScreen
import com.accu.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() }
    }

    Scaffold(
        topBar = { ACCTopBar(title = "Privacy Center", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = PrivacyTab.entries.indexOf(state.selectedTab)) {
                PrivacyTab.entries.forEach { tab ->
                    Tab(selected = state.selectedTab == tab, onClick = { viewModel.onTabChange(tab) }, text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            when (state.selectedTab) {
                PrivacyTab.DASHBOARD   -> PrivacyDashboard(state = state, viewModel = viewModel)
                PrivacyTab.TRACKERS    -> TrackerBlockerTab(state = state, viewModel = viewModel)
                PrivacyTab.COMPONENTS  -> ComponentsTab(state = state, viewModel = viewModel)
                PrivacyTab.RULES       -> RulesTab(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun PrivacyDashboard(state: PrivacyUiState, viewModel: PrivacyViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Stats
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrivacyStatCard("Blocked Components", "${state.blockedCount}", AccentRed, Modifier.weight(1f))
                PrivacyStatCard("Trackers Blocked", "${state.trackerCount}", AccentOrange, Modifier.weight(1f))
                PrivacyStatCard("Rules Active", "${state.privacyRules.count { it.isEnabled }}", AccentGreen, Modifier.weight(1f))
            }
        }

        // Quick actions
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Block All Analytics" to { viewModel.blockTrackersInCategory("Analytics") },
                        "Block All Ads" to { viewModel.blockTrackersInCategory("Ads") },
                        "Block Social Trackers" to { viewModel.blockTrackersInCategory("Social") },
                        "Block Crash Reporters" to { viewModel.blockTrackersInCategory("Crash Reporting") },
                    ).forEach { (label, action) ->
                        OutlinedButton(onClick = action, Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Icon(Icons.Default.Block, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            }
        }

        // IFW rules info
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("About Component Blocking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ACC uses Intent Firewall (IFW) rules and pm disable to block trackers and components. " +
                        "Unlike traditional methods, IFW blocks are invisible to the app — it never even receives the intent. " +
                        "This is more secure than simply disabling components via pm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerBlockerTab(state: PrivacyUiState, viewModel: PrivacyViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.trackerCategories) { cat ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${cat.trackerCount} trackers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cat.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { viewModel.blockTrackersInCategory(cat.name) }) { Text("Block All") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Exodus Privacy Integration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Tracker signatures sourced from Exodus Privacy database. Blocks at component level — apps cannot detect the block.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ComponentsTab(state: PrivacyUiState, viewModel: PrivacyViewModel) {
    if (state.blockedComponents.isEmpty()) {
        EmptyState(Icons.Default.Shield, "No blocked components", "Block trackers or disable components from App Manager")
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.blockedComponents, key = { "${it.packageName}/${it.componentName}" }) { comp ->
                ListItem(
                    headlineContent = { Text(comp.componentName.substringAfterLast('.')) },
                    supportingContent = { Column { Text(comp.packageName, style = MaterialTheme.typography.bodySmall); Text("${comp.componentType} · ${comp.ruleSource}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    leadingContent = { Icon(if (comp.isTracker) Icons.Default.TrackChanges else Icons.Default.Block, null, tint = if (comp.isTracker) AccentOrange else MaterialTheme.colorScheme.primary) },
                    trailingContent = { IconButton(onClick = { viewModel.enableComponent(comp.packageName, comp.componentName) }) { Icon(Icons.Default.PlayArrow, "Enable") } },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RulesTab(state: PrivacyUiState, viewModel: PrivacyViewModel) {
    if (state.privacyRules.isEmpty()) {
        EmptyState(Icons.Default.Rule, "No custom rules", "Privacy rules appear here when you create them")
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.privacyRules, key = { it.id }) { rule ->
                ListItem(
                    headlineContent = { Text(rule.ruleName) },
                    supportingContent = { Text("${rule.ruleType} · ${rule.packageName}", style = MaterialTheme.typography.bodySmall) },
                    trailingContent = {
                        Row {
                            Switch(checked = rule.isEnabled, onCheckedChange = {})
                            IconButton(onClick = { viewModel.deleteRule(rule) }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PrivacyStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
