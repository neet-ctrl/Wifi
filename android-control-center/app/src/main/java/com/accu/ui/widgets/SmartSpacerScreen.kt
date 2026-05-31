package com.accu.ui.widgets

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.accu.data.db.dao.SmartSpacerPluginDao
import com.accu.data.db.entities.SmartSpacerPluginEntity
import com.accu.ui.components.ACCTopBar
import com.accu.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartSpacerState(
    val plugins: List<SmartSpacerPluginEntity> = emptyList(),
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null,
)

@HiltViewModel
class SmartSpacerViewModel @Inject constructor(
    private val pluginDao: SmartSpacerPluginDao,
) : ViewModel() {
    private val _state = MutableStateFlow(SmartSpacerState())
    val state: StateFlow<SmartSpacerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            pluginDao.observeAll().collect { plugins -> _state.update { it.copy(plugins = plugins, isLoading = false) } }
        }
        seedDefaultPlugins()
    }

    private fun seedDefaultPlugins() {
        viewModelScope.launch {
            val existing = pluginDao.observeAll().firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    SmartSpacerPluginEntity("weather", "Weather", "Current weather conditions", displayOrder = 0),
                    SmartSpacerPluginEntity("calendar", "Calendar", "Next calendar event", displayOrder = 1),
                    SmartSpacerPluginEntity("music", "Now Playing", "Currently playing music", displayOrder = 2),
                    SmartSpacerPluginEntity("steps", "Step Counter", "Daily step count", displayOrder = 3),
                    SmartSpacerPluginEntity("battery", "Battery", "Battery level and charging status", displayOrder = 4),
                    SmartSpacerPluginEntity("notifications", "Notification Count", "Unread notification count", displayOrder = 5),
                    SmartSpacerPluginEntity("shortcuts", "App Shortcuts", "Quick app shortcuts", displayOrder = 6),
                    SmartSpacerPluginEntity("datetime", "Date & Time", "Enhanced date/time display", displayOrder = 7),
                )
                defaults.forEach { pluginDao.insert(it) }
            }
        }
    }

    fun togglePlugin(id: String, enabled: Boolean) { viewModelScope.launch { pluginDao.setEnabled(id, enabled) } }
    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSpacerScreen(
    onBack: () -> Unit,
    viewModel: SmartSpacerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) { state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() } }

    Scaffold(
        topBar = { ACCTopBar(title = "Smart Widgets", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("At-a-Glance Enhancements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Extend the Google At-a-Glance widget and lock screen with custom plugins. Based on SmartSpacer.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("⚠ Requires Shizuku or root for full functionality.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            items(state.plugins, key = { it.id }) { plugin ->
                val surfaceIcon = when (plugin.targetSurface) {
                    "lockscreen" -> Icons.Default.Lock
                    "expanded" -> Icons.Default.OpenInFull
                    else -> Icons.Default.Widgets
                }
                ListItem(
                    headlineContent = { Text(plugin.name, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(plugin.description, style = MaterialTheme.typography.bodySmall) },
                    leadingContent = {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (plugin.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(surfaceIcon, null, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    trailingContent = {
                        Switch(checked = plugin.isEnabled, onCheckedChange = { viewModel.togglePlugin(plugin.id, it) })
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
