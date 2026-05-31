package com.accu.ui.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.utils.ShizukuUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class NetworkUiState(
    val wifiEnabled: Boolean = false,
    val mobileDataEnabled: Boolean = false,
    val hotspotEnabled: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val nfcEnabled: Boolean = false,
    val airplaneModeEnabled: Boolean = false,
    val wifiSsid: String = "",
    val wifiIp: String = "",
    val wifiMac: String = "",
    val wifiSignal: Int = 0,
    val wifiLinkSpeed: Int = 0,
    val isLoading: Boolean = true,
    val cmdOutput: String = "",
    val snackbarMessage: String? = null,
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuUtils: ShizukuUtils,
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkUiState())
    val state: StateFlow<NetworkUiState> = _state.asStateFlow()

    init { loadNetworkState() }

    private fun loadNetworkState() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cm = context.getSystemService(ConnectivityManager::class.java)
                val wifiManager = context.getSystemService(WifiManager::class.java)
                val wifiEnabled = wifiManager?.isWifiEnabled ?: false
                val wifiInfo = wifiManager?.connectionInfo
                val network = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(network)
                val hasMobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
                val ssid = wifiInfo?.ssid?.removeSurrounding("\"") ?: ""
                val ip = intToIp(wifiInfo?.ipAddress ?: 0)
                val signal = wifiInfo?.rssi ?: 0
                val linkSpeed = wifiInfo?.linkSpeed ?: 0

                _state.update {
                    it.copy(
                        wifiEnabled = wifiEnabled,
                        mobileDataEnabled = hasMobile,
                        wifiSsid = ssid,
                        wifiIp = ip,
                        wifiSignal = signal,
                        wifiLinkSpeed = linkSpeed,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleWifi() {
        viewModelScope.launch {
            val result = shizukuUtils.execShizuku("svc wifi ${if (_state.value.wifiEnabled) "disable" else "enable"}")
            if (result.isSuccess) {
                _state.update { it.copy(wifiEnabled = !it.wifiEnabled, snackbarMessage = "Wi-Fi ${if (!it.wifiEnabled) "enabled" else "disabled"}") }
            } else {
                _state.update { it.copy(snackbarMessage = "Failed: ${result.error}") }
            }
        }
    }

    fun toggleMobileData() {
        viewModelScope.launch {
            val result = shizukuUtils.execShizuku("svc data ${if (_state.value.mobileDataEnabled) "disable" else "enable"}")
            if (result.isSuccess) {
                _state.update { it.copy(mobileDataEnabled = !it.mobileDataEnabled, snackbarMessage = "Mobile data ${if (!it.mobileDataEnabled) "enabled" else "disabled"}") }
            } else {
                _state.update { it.copy(snackbarMessage = "Failed: ${result.error}") }
            }
        }
    }

    fun toggleHotspot() {
        viewModelScope.launch {
            val result = if (_state.value.hotspotEnabled) {
                shizukuUtils.execShizuku("cmd wifi stop-softap")
            } else {
                shizukuUtils.execShizuku("cmd wifi start-softap")
            }
            if (result.isSuccess) {
                _state.update { it.copy(hotspotEnabled = !it.hotspotEnabled) }
            } else {
                _state.update { it.copy(snackbarMessage = "Hotspot toggle failed (may need root)") }
            }
        }
    }

    fun toggleBluetooth() {
        viewModelScope.launch {
            val result = shizukuUtils.execShizuku("svc bluetooth ${if (_state.value.bluetoothEnabled) "disable" else "enable"}")
            if (result.isSuccess) _state.update { it.copy(bluetoothEnabled = !it.bluetoothEnabled) }
        }
    }

    fun toggleNfc() {
        viewModelScope.launch {
            val result = shizukuUtils.execShizuku("svc nfc ${if (_state.value.nfcEnabled) "disable" else "enable"}")
            if (result.isSuccess) _state.update { it.copy(nfcEnabled = !it.nfcEnabled) }
            else _state.update { it.copy(snackbarMessage = "NFC toggle requires root") }
        }
    }

    fun toggleAirplaneMode() {
        viewModelScope.launch {
            val newVal = if (_state.value.airplaneModeEnabled) "0" else "1"
            val result = shizukuUtils.execShizuku("settings put global airplane_mode_on $newVal && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ${newVal == "1"}")
            if (result.isSuccess) _state.update { it.copy(airplaneModeEnabled = !it.airplaneModeEnabled) }
        }
    }

    fun executeCmd(cmd: String) {
        viewModelScope.launch {
            val result = shizukuUtils.execShizuku(cmd)
            _state.update { it.copy(cmdOutput = result.combinedOutput) }
        }
    }

    fun scanNetworks() { executeCmd("cmd wifi list-scan-results") }
    fun forgetWifi() { executeCmd("cmd wifi forget-network 0") }
    fun addQsTile(tile: String) { _state.update { it.copy(snackbarMessage = "Add '$tile' from Quick Settings edit mode") } }
    fun clearOutput() { _state.update { it.copy(cmdOutput = "") } }
    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }

    private fun intToIp(i: Int): String {
        if (i == 0) return ""
        return "${i and 0xff}.${i shr 8 and 0xff}.${i shr 16 and 0xff}.${i shr 24 and 0xff}"
    }
}
