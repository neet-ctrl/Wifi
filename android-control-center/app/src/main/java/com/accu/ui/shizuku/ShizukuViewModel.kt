package com.accu.ui.shizuku

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.utils.ShizukuUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import rikka.shizuku.Shizuku
import timber.log.Timber
import javax.inject.Inject

data class ShizukuUiState(
    val isAvailable: Boolean = false,
    val isGranted: Boolean = false,
    val isRootAvailable: Boolean = false,
    val version: Int = -1,
    val uid: Int = -1,
    val isInstalled: Boolean = false,
    val logs: List<ShizukuLogEntry> = emptyList(),
    val authorizedApps: List<AuthorizedApp> = emptyList(),
    val wirelessAdbPort: Int = 0,
    val isWirelessAdbEnabled: Boolean = false,
    val deviceIp: String = "",
    val pairingCode: String = "",
    val isPairing: Boolean = false,
    val pairingStatus: String = "",
    val isLoading: Boolean = true,
)

data class ShizukuLogEntry(val timestamp: Long = System.currentTimeMillis(), val message: String, val level: LogLevel = LogLevel.INFO)
enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

data class AuthorizedApp(val packageName: String, val appName: String, val uid: Int)

@HiltViewModel
class ShizukuViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuUtils: ShizukuUtils,
) : ViewModel() {

    private val _state = MutableStateFlow(ShizukuUiState())
    val state: StateFlow<ShizukuUiState> = _state.asStateFlow()

    private val binderListener = Shizuku.OnBinderReceivedListener {
        viewModelScope.launch { refresh() }
    }
    private val deadListener = Shizuku.OnBinderDeadListener {
        _state.update { it.copy(isAvailable = false, isGranted = false) }
        addLog("Shizuku binder died", LogLevel.ERROR)
    }
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
        _state.update { it.copy(isGranted = granted) }
        addLog(if (granted) "Permission granted" else "Permission denied", if (granted) LogLevel.SUCCESS else LogLevel.ERROR)
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(deadListener)
        Shizuku.addRequestPermissionResultListener(permListener)
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        withContext(Dispatchers.IO) {
            val available = shizukuUtils.isShizukuAvailable()
            val granted = if (available) shizukuUtils.isShizukuGranted() else false
            val version = if (available) shizukuUtils.getShizukuVersion() else -1
            val uid = if (available) shizukuUtils.getShizukuUid() else -1
            val isRoot = shizukuUtils.isRootAvailable()
            val installed = shizukuUtils.isShizukuInstalled(context)
            val deviceIp = getDeviceIp()
            val port = getAdbPort()

            _state.update {
                it.copy(
                    isAvailable = available, isGranted = granted,
                    version = version, uid = uid,
                    isRootAvailable = isRoot, isInstalled = installed,
                    deviceIp = deviceIp, wirelessAdbPort = port,
                    isLoading = false,
                )
            }
            if (available) addLog("Shizuku v$version running (uid=$uid)", LogLevel.SUCCESS)
        }
    }

    fun requestPermission() {
        addLog("Requesting Shizuku permission…", LogLevel.INFO)
        shizukuUtils.requestShizukuPermission()
    }

    fun startWithAdb() {
        viewModelScope.launch {
            addLog("Starting Shizuku via ADB…", LogLevel.INFO)
            val result = shizukuUtils.execAdb("adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh")
            addLog(if (result.isSuccess) "Shizuku started" else "Failed: ${result.error}", if (result.isSuccess) LogLevel.SUCCESS else LogLevel.ERROR)
        }
    }

    fun startWithRoot() {
        viewModelScope.launch {
            addLog("Starting Shizuku via root…", LogLevel.INFO)
            val result = shizukuUtils.execRoot("sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh")
            addLog(if (result.isSuccess) "Shizuku started via root" else "Failed: ${result.error}", if (result.isSuccess) LogLevel.SUCCESS else LogLevel.ERROR)
            delay(1500)
            refresh()
        }
    }

    fun enableWirelessAdb() {
        viewModelScope.launch {
            addLog("Enabling wireless ADB on port 5555…", LogLevel.INFO)
            val result = shizukuUtils.execShizuku("settings put global adb_wifi_enabled 1")
            val result2 = shizukuUtils.execShizuku("setprop service.adb.tcp.port 5555")
            if (result.isSuccess) {
                _state.update { it.copy(isWirelessAdbEnabled = true, wirelessAdbPort = 5555) }
                addLog("Wireless ADB enabled on port 5555", LogLevel.SUCCESS)
            } else {
                addLog("Failed: ${result.error}", LogLevel.ERROR)
            }
        }
    }

    fun disableWirelessAdb() {
        viewModelScope.launch {
            shizukuUtils.execShizuku("settings put global adb_wifi_enabled 0")
            _state.update { it.copy(isWirelessAdbEnabled = false) }
            addLog("Wireless ADB disabled", LogLevel.INFO)
        }
    }

    fun startPairing(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isPairing = true, pairingStatus = "Pairing…") }
            addLog("Starting ADB pairing with code: $code", LogLevel.INFO)
            val result = shizukuUtils.execShizuku("adb pair 127.0.0.1:${_state.value.wirelessAdbPort} $code")
            _state.update { it.copy(isPairing = false, pairingStatus = if (result.isSuccess) "Paired!" else "Failed") }
            addLog(if (result.isSuccess) "Paired successfully" else "Pairing failed: ${result.error}",
                if (result.isSuccess) LogLevel.SUCCESS else LogLevel.ERROR)
        }
    }

    fun revokeApp(packageName: String) {
        viewModelScope.launch {
            shizukuUtils.execShizuku("pm revoke $packageName moe.shizuku.manager.permission.API_V23")
            addLog("Revoked Shizuku access for $packageName", LogLevel.INFO)
            refresh()
        }
    }

    fun clearLogs() { _state.update { it.copy(logs = emptyList()) } }

    private fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        _state.update { s -> s.copy(logs = (s.logs + ShizukuLogEntry(message = message, level = level)).takeLast(200)) }
    }

    private fun getDeviceIp(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()?.toList() ?: return ""
            interfaces.flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && it is java.net.Inet4Address }
                .firstOrNull()?.hostAddress ?: ""
        } catch (_: Exception) { "" }
    }

    private fun getAdbPort(): Int = try {
        val result = shizukuUtils.execAdb("getprop service.adb.tcp.port")
        result.output.trim().toIntOrNull() ?: 5555
    } catch (_: Exception) { 5555 }

    override fun onCleared() {
        super.onCleared()
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(deadListener)
        Shizuku.removeRequestPermissionResultListener(permListener)
    }
}
