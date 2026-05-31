package com.example.accu.fulldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.accu.sdk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ACCU SDK — Full Demo App
 *
 * A complete reference app showing:
 *  1. Connection lifecycle & state management
 *  2. Permission request dialog flow
 *  3. Shell execution (sync + async)
 *  4. Package management (disable/enable/hide/force-stop)
 *  5. Runtime permission grant/revoke
 *  6. Settings read/write (Secure + Global + System)
 *  7. Per-app locale override
 *  8. Scope checking
 *  9. Disconnect / reconnect handling
 * 10. Error recovery
 */

// ── ViewModel ────────────────────────────────────────────────────────────────

class FullDemoViewModel(app: android.app.Application) :
    androidx.lifecycle.AndroidViewModel(app) {

    private val accu = AccuClient(app)
    val accuState: StateFlow<AccuConnectionState> = accu.state

    private val _log = MutableStateFlow<List<LogEntry>>(listOf(LogEntry("System", "FullDemoApp started")))
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    data class LogEntry(val tag: String, val message: String)

    init { accu.connect() }
    override fun onCleared() { accu.disconnect() }

    private fun log(tag: String, msg: String) {
        _log.update { (it + LogEntry(tag, msg)).takeLast(100) }
    }

    fun requestPermission() { viewModelScope.launch {
        log("Permission", "Requesting... (dialog will appear)")
        val result = accu.requestPermission()
        log("Permission", result.toPermissionLabel())
    }}

    fun ping() { viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) { accu.ping() }
        log("Ping", if (ok) "✅ alive" else "❌ dead")
    }}

    fun identity() { viewModelScope.launch {
        withContext(Dispatchers.IO) {
            log("Identity", "version=${accu.getVersion()} uid=${accu.getUid()}")
        }
    }}

    fun checkScopes() { viewModelScope.launch {
        withContext(Dispatchers.IO) {
            AccuScopes.ALL_SCOPES.forEach { scope ->
                log("Scope", "$scope → ${if (accu.hasScope(scope)) "✅" else "❌"}")
            }
        }
    }}

    fun execId() { viewModelScope.launch {
        val r = withContext(Dispatchers.IO) { accu.exec("id") }
        log("Shell", "exit=${r.exitCode} out=${r.stdout.trim()}")
    }}

    fun execAsync() { viewModelScope.launch {
        log("StreamShell", "Starting dmesg stream...")
        withContext(Dispatchers.IO) {
            accu.execAsync("dmesg | tail -5",
                onStdout = { log("StreamShell", it) },
                onStderr = { log("StreamShell", "ERR: $it") },
                onExit   = { log("StreamShell", "exit=$it") },
            )
        }
    }}

    fun disableGoogleChrome() { viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) { accu.disablePackage("com.android.chrome") }
        log("Pkg", "disable Chrome: ${if (ok) "✅" else "❌"}")
    }}

    fun enableGoogleChrome() { viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) { accu.enablePackage("com.android.chrome") }
        log("Pkg", "enable Chrome: ${if (ok) "✅" else "❌"}")
    }}

    fun hideAndRestore(pkg: String) { viewModelScope.launch {
        withContext(Dispatchers.IO) {
            log("Pkg", "hide $pkg: ${accu.hidePackage(pkg)}")
            Thread.sleep(2000)
            log("Pkg", "unhide $pkg: ${accu.unhidePackage(pkg)}")
        }
    }}

    fun grantLocationToMaps() { viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            accu.grantPermission("com.google.android.apps.maps", "android.permission.ACCESS_FINE_LOCATION")
        }
        log("Perm", "grant FINE_LOCATION to Maps: ${if (ok) "✅" else "❌"}")
    }}

    fun disableAnimations() { viewModelScope.launch {
        withContext(Dispatchers.IO) {
            listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
                .forEach { accu.writeGlobalSetting(it, "0") }
            log("Settings", "Animations disabled")
        }
    }}

    fun readBluetooth() { viewModelScope.launch {
        val v = withContext(Dispatchers.IO) { accu.readSecureSetting("bluetooth_on") }
        log("Settings", "bluetooth_on = $v")
    }}

    fun setChromeFrench() { viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            accu.setApplicationLocale("com.android.chrome", "fr-FR")
        }
        log("Locale", "Chrome → fr-FR: ${if (ok) "✅" else "❌"}")
    }}

    fun revokeSelf() { viewModelScope.launch {
        accu.revokeSelf()
        log("Permission", "Self-revoked. Must request again.")
    }}
}

// ── UI ───────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private val vm: FullDemoViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FullDemoScreen(vm) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullDemoScreen(vm: FullDemoViewModel) {
    val state by vm.accuState.collectAsState()
    val log   by vm.log.collectAsState()
    var tab   by remember { mutableIntStateOf(0) }
    val tabs = listOf("Connection", "Shell", "Packages", "Settings", "Log")

    Scaffold(
        topBar = { TopAppBar(title = { Text("ACCU Full Demo") }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Status bar
            Surface(color = when (state) {
                is AccuConnectionState.Connected    -> MaterialTheme.colorScheme.primaryContainer
                is AccuConnectionState.Error        -> MaterialTheme.colorScheme.errorContainer
                is AccuConnectionState.Disconnected -> MaterialTheme.colorScheme.tertiaryContainer
                else                                -> MaterialTheme.colorScheme.surfaceVariant
            }) {
                Text(
                    text = when (val s = state) {
                        is AccuConnectionState.Connected    -> "✅ Connected — ACCU ${s.accuVersion} | ${s.permissionCode.toPermissionLabel()}"
                        is AccuConnectionState.Error        -> "❌ ${s.reason}"
                        is AccuConnectionState.Connecting   -> "⏳ Connecting..."
                        is AccuConnectionState.Disconnected -> "🔴 Disconnected (auto-reconnecting)"
                        else                                -> "⚪ Idle"
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            ScrollableTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }

            val ready = (state as? AccuConnectionState.Connected)?.isPermissionGranted == true

            when (tab) {
                0 -> ConnectionTab(state, vm, ready)
                1 -> ShellTab(vm, ready)
                2 -> PackagesTab(vm, ready)
                3 -> SettingsTab(vm, ready)
                4 -> LogTab(log)
            }
        }
    }
}

@Composable
fun ConnectionTab(state: AccuConnectionState, vm: FullDemoViewModel, ready: Boolean) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state is AccuConnectionState.Connected && !state.isPermissionGranted) {
            Button(onClick = { vm.requestPermission() }, modifier = Modifier.fillMaxWidth()) {
                Text("Request ACCU Permission")
            }
        }
        Button(onClick = { vm.ping() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Ping Service") }
        Button(onClick = { vm.identity() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Get Identity (version, uid)") }
        Button(onClick = { vm.checkScopes() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Check All Scopes") }
        OutlinedButton(onClick = { vm.revokeSelf() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Revoke My Permission") }
    }
}

@Composable
fun ShellTab(vm: FullDemoViewModel, ready: Boolean) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { vm.execId() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("exec: id (sync)") }
        Button(onClick = { vm.execAsync() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("execAsync: dmesg tail (streaming)") }
    }
}

@Composable
fun PackagesTab(vm: FullDemoViewModel, ready: Boolean) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Target: com.android.chrome", style = MaterialTheme.typography.labelMedium)
        Button(onClick = { vm.disableGoogleChrome() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Disable Chrome") }
        Button(onClick = { vm.enableGoogleChrome() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Enable Chrome") }
        Button(onClick = { vm.hideAndRestore("com.android.chrome") }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Hide Chrome → wait 2s → Unhide") }
        HorizontalDivider()
        Text("Target: com.google.android.apps.maps", style = MaterialTheme.typography.labelMedium)
        Button(onClick = { vm.grantLocationToMaps() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Grant FINE_LOCATION to Maps") }
    }
}

@Composable
fun SettingsTab(vm: FullDemoViewModel, ready: Boolean) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { vm.disableAnimations() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Disable System Animations") }
        Button(onClick = { vm.readBluetooth() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Read bluetooth_on (Secure)") }
        Button(onClick = { vm.setChromeFrench() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Set Chrome locale → fr-FR") }
    }
}

@Composable
fun LogTab(log: List<FullDemoViewModel.LogEntry>) {
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        items(log.reversed()) { entry ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("[${entry.tag}]", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(90.dp))
                Text(entry.message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
