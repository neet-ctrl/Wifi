package com.accu.ui.customization

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.accu.ui.components.ACCTopBar
import com.accu.utils.ShizukuUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DarkModeState(
    val apps: List<DarkModeApp> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val globalDarkForced: Boolean = false,
    val snackbarMessage: String? = null,
)
data class DarkModeApp(val packageName: String, val appName: String, val forceDark: Boolean = false, val isSystemApp: Boolean = false)

@HiltViewModel
class DarkModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuUtils: ShizukuUtils,
) : ViewModel() {
    private val _state = MutableStateFlow(DarkModeState())
    val state: StateFlow<DarkModeState> = _state.asStateFlow()

    init { loadApps() }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledPackages(0).mapNotNull { pkg ->
                val ai = pkg.applicationInfo ?: return@mapNotNull null
                DarkModeApp(pkg.packageName, pm.getApplicationLabel(ai).toString(), isSystemApp = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
            }.sortedBy { it.appName }
            _state.update { it.copy(apps = apps, isLoading = false) }
        }
    }

    fun toggleForceDark(packageName: String) {
        viewModelScope.launch {
            val app = _state.value.apps.firstOrNull { it.packageName == packageName } ?: return@launch
            val newState = !app.forceDark
            // DarQ approach: set via settings or accessibility overlay
            val result = shizukuUtils.execShizuku("settings put secure debug.hwui.force_dark_mode_${packageName} ${if (newState) "1" else "0"}")
            _state.update { s -> s.copy(apps = s.apps.map { if (it.packageName == packageName) it.copy(forceDark = newState) else it }) }
        }
    }

    fun toggleGlobalDark() {
        viewModelScope.launch {
            val newState = !_state.value.globalDarkForced
            shizukuUtils.execShizuku("settings put secure ui_night_mode ${if (newState) "2" else "1"}")
            _state.update { it.copy(globalDarkForced = newState, snackbarMessage = if (newState) "System dark mode forced" else "System dark mode reset") }
        }
    }

    fun onSearch(q: String) { _state.update { it.copy(searchQuery = q) } }
    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkModeScreen(
    onBack: () -> Unit,
    viewModel: DarkModeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) { state.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnackbar() } }

    Scaffold(topBar = { ACCTopBar(title = "Per-App Dark Mode", onBack = onBack) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Global Force Dark", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Force dark mode system-wide (may break some apps)", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.globalDarkForced, onCheckedChange = { viewModel.toggleGlobalDark() })
                }
            }
            SearchBar(query = state.searchQuery, onQueryChange = viewModel::onSearch, onSearch = {}, active = false, onActiveChange = {}, placeholder = { Text("Search apps…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {}
            val filtered = state.apps.filter { it.appName.contains(state.searchQuery, true) || it.packageName.contains(state.searchQuery, true) }
            LazyColumn(Modifier.weight(1f)) {
                items(filtered, key = { it.packageName }) { app ->
                    ListItem(
                        headlineContent = { Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = { AsyncImage(model = ImageRequest.Builder(context).data(try { context.packageManager.getApplicationIcon(app.packageName) } catch (_: Exception) { null }).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(40.dp)) },
                        trailingContent = { Switch(checked = app.forceDark, onCheckedChange = { viewModel.toggleForceDark(app.packageName) }) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
