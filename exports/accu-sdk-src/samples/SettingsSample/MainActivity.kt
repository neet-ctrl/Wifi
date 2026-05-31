package com.example.accu.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.accu.sdk.AccuClient
import com.accu.sdk.AccuConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ACCU SDK — Settings Sample
 *
 * Demonstrates reading and writing Android system settings via ACCU:
 *  - Settings.Secure (e.g. bluetooth_on, location_providers_allowed)
 *  - Settings.Global (e.g. animator_duration_scale)
 *  - Settings.System (e.g. screen_brightness)
 *  - setApplicationLocale() for per-app language overrides
 */
class SettingsViewModel(app: android.app.Application) : androidx.lifecycle.AndroidViewModel(app) {
    private val accu = AccuClient(app)
    val accuState: StateFlow<AccuConnectionState> = accu.state

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    init { accu.connect() }
    override fun onCleared() { accu.disconnect() }
    fun requestPermission() { viewModelScope.launch { accu.requestPermission() } }

    private fun addLog(msg: String) { _log.value = _log.value + msg }

    fun disableAnimations() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val ok1 = accu.writeGlobalSetting("window_animation_scale",     "0")
                val ok2 = accu.writeGlobalSetting("transition_animation_scale",  "0")
                val ok3 = accu.writeGlobalSetting("animator_duration_scale",     "0")
                addLog("Disable animations: window=$ok1 transition=$ok2 animator=$ok3")
            }
        }
    }

    fun restoreAnimations() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                accu.writeGlobalSetting("window_animation_scale",     "1")
                accu.writeGlobalSetting("transition_animation_scale",  "1")
                accu.writeGlobalSetting("animator_duration_scale",     "1")
                addLog("Animations restored to 1x")
            }
        }
    }

    fun readBrightness() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val value = accu.readSystemSetting("screen_brightness")
                addLog("screen_brightness = $value")
            }
        }
    }

    fun setBrightness(value: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val ok = accu.writeSystemSetting("screen_brightness", value.toString())
                addLog("Set brightness to $value: $ok")
            }
        }
    }

    fun setLocale(packageName: String, locale: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val ok = accu.setApplicationLocale(packageName, locale)
                addLog("setApplicationLocale($packageName, $locale): $ok")
            }
        }
    }

    fun readSecure(key: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val value = accu.readSecureSetting(key)
                addLog("secure/$key = $value")
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { SettingsScreen(vm) } }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val state by vm.accuState.collectAsState()
    val log by vm.log.collectAsState()
    var pkg by remember { mutableStateOf("com.example.app") }
    var locale by remember { mutableStateOf("ja-JP") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Settings Sample", style = MaterialTheme.typography.headlineSmall)

        when (val s = state) {
            is AccuConnectionState.Connected ->
                if (!s.isPermissionGranted) Button(onClick = { vm.requestPermission() }) { Text("Grant ACCU Permission") }
            is AccuConnectionState.Error -> Text("Error: ${s.reason}", color = MaterialTheme.colorScheme.error)
            else -> LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        val ready = (state as? AccuConnectionState.Connected)?.isPermissionGranted == true

        Button(onClick = { vm.disableAnimations() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Disable Animations (developer trick)") }
        Button(onClick = { vm.restoreAnimations() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Restore Animations") }
        Button(onClick = { vm.readBrightness() }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Read screen_brightness") }
        Button(onClick = { vm.setBrightness(128) }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Set Brightness = 128") }
        Button(onClick = { vm.readSecure("bluetooth_on") }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Read bluetooth_on (Secure)") }

        HorizontalDivider()
        OutlinedTextField(pkg, { pkg = it }, label = { Text("Package for locale") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(locale, { locale = it }, label = { Text("Locale (BCP 47, empty = reset)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { vm.setLocale(pkg, locale) }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Set Per-App Locale") }

        Card(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.padding(12.dp)) {
                log.takeLast(20).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
