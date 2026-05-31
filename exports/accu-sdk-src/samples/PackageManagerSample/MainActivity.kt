package com.example.accu.pkgmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.accu.sdk.AccuClient
import com.accu.sdk.AccuConnectionState
import com.accu.sdk.AccuConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ACCU SDK — Package Manager Sample
 *
 * Demonstrates:
 *  - disablePackage() / enablePackage()
 *  - hidePackage() / unhidePackage()
 *  - forceStop()
 *  - clearPackageData()
 *  - grantPermission() / revokePermission()
 */
class PkgViewModel(app: android.app.Application) : androidx.lifecycle.AndroidViewModel(app) {
    private val accu = AccuClient(app)
    val accuState: StateFlow<AccuConnectionState> = accu.state

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result.asStateFlow()

    init { accu.connect() }
    override fun onCleared() { accu.disconnect() }

    fun requestPermission() { viewModelScope.launch { accu.requestPermission() } }

    private fun run(label: String, block: () -> Boolean) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { block() }.getOrElse { false } }
            _result.value = "$label → ${if (ok) "✅ success" else "❌ failed"}"
        }
    }

    fun disableApp(pkg: String)        = run("disable $pkg")   { accu.disablePackage(pkg) }
    fun enableApp(pkg: String)         = run("enable $pkg")    { accu.enablePackage(pkg) }
    fun hideApp(pkg: String)           = run("hide $pkg")      { accu.hidePackage(pkg) }
    fun unhideApp(pkg: String)         = run("unhide $pkg")    { accu.unhidePackage(pkg) }
    fun forceStopApp(pkg: String)      = run("force-stop $pkg"){ accu.forceStop(pkg) }
    fun clearData(pkg: String)         = run("clear $pkg")     { accu.clearPackageData(pkg) }
    fun grantCam(pkg: String)          = run("grant CAMERA")   { accu.grantPermission(pkg, "android.permission.CAMERA") }
    fun revokeCam(pkg: String)         = run("revoke CAMERA")  { accu.revokePermission(pkg, "android.permission.CAMERA") }
}

class MainActivity : ComponentActivity() {
    private val vm: PkgViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PkgScreen(vm) } }
    }
}

@Composable
fun PkgScreen(vm: PkgViewModel) {
    val state by vm.accuState.collectAsState()
    val result by vm.result.collectAsState()
    var pkg by remember { mutableStateOf("com.example.targetapp") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Package Manager Sample", style = MaterialTheme.typography.headlineSmall)

        when (val s = state) {
            is AccuConnectionState.Connected ->
                if (!s.isPermissionGranted) Button(onClick = { vm.requestPermission() }) { Text("Grant ACCU Permission") }
            is AccuConnectionState.Error -> Text("Error: ${s.reason}", color = MaterialTheme.colorScheme.error)
            else -> LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        OutlinedTextField(value = pkg, onValueChange = { pkg = it }, label = { Text("Target Package") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        val isReady = (state as? AccuConnectionState.Connected)?.isPermissionGranted == true

        @Composable fun ActionButton(label: String, action: () -> Unit) =
            Button(onClick = action, enabled = isReady, modifier = Modifier.fillMaxWidth()) { Text(label) }

        ActionButton("Disable App")        { vm.disableApp(pkg) }
        ActionButton("Enable App")         { vm.enableApp(pkg) }
        ActionButton("Hide App")           { vm.hideApp(pkg) }
        ActionButton("Unhide App")         { vm.unhideApp(pkg) }
        ActionButton("Force Stop")         { vm.forceStopApp(pkg) }
        ActionButton("Clear App Data")     { vm.clearData(pkg) }
        ActionButton("Grant CAMERA")       { vm.grantCam(pkg) }
        ActionButton("Revoke CAMERA")      { vm.revokeCam(pkg) }

        if (result.isNotBlank()) {
            Card(Modifier.fillMaxWidth()) {
                Text(result, Modifier.padding(12.dp))
            }
        }
    }
}
