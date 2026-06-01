package com.airkey.wifiqr.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airkey.wifiqr.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val networks: List<WifiNetwork> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val toastMessage: String? = null,
    val networkCount: Int = 0
)

data class QrStyle(
    val patternIndex: Int = 0,
    val foregroundColor: Long = 0xFF6C63FF,
    val backgroundColor: Long = 0xFF0A0A1A,
    val accentColor: Long = 0xFF00F5FF,
    val dotShape: Int = 0,
    val frameStyle: Int = 0,
    val showLogo: Boolean = false,
    val logoText: String = "AK"
)

class WifiViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WifiDatabase.getDatabase(application)
    private val repo = WifiRepository(db.wifiDao())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")

    private val _scannedResult = MutableStateFlow<ScannedWifiResult?>(null)
    val scannedResult: StateFlow<ScannedWifiResult?> = _scannedResult.asStateFlow()

    private val _qrStyle = MutableStateFlow(QrStyle())
    val qrStyle: StateFlow<QrStyle> = _qrStyle.asStateFlow()

    private val _editingNetwork = MutableStateFlow<WifiNetwork?>(null)
    val editingNetwork: StateFlow<WifiNetwork?> = _editingNetwork.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _connectMessage = MutableStateFlow<String?>(null)
    val connectMessage: StateFlow<String?> = _connectMessage.asStateFlow()

    val categories = listOf("All", "Home", "Work", "Travel", "Public", "Guest", "General")

    private var activeNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var activeConnectivityManager: ConnectivityManager? = null

    init {
        viewModelScope.launch {
            combine(repo.allNetworks, _searchQuery, _selectedCategory) { all, query, category ->
                all.filter { n ->
                    (query.isBlank() || n.ssid.contains(query, ignoreCase = true) ||
                            n.notes.contains(query, ignoreCase = true)) &&
                            (category == "All" || n.category == category)
                }
            }.collect { networks ->
                _uiState.update {
                    it.copy(networks = networks, networkCount = networks.size)
                }
            }
        }
    }

    fun onSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun saveNetwork(network: WifiNetwork) {
        viewModelScope.launch {
            val existing = repo.getBySsid(network.ssid)
            if (existing != null) {
                repo.update(network.copy(id = existing.id, savedAt = existing.savedAt))
            } else {
                repo.insert(network)
            }
        }
    }

    fun deleteNetwork(network: WifiNetwork) {
        viewModelScope.launch { repo.delete(network) }
    }

    fun toggleFavorite(network: WifiNetwork) {
        viewModelScope.launch { repo.update(network.copy(isFavorite = !network.isFavorite)) }
    }

    fun onQrScanned(raw: String) {
        _scannedResult.value = parseWifiQrCode(raw)
    }

    fun clearScannedResult() {
        _scannedResult.value = null
    }

    fun setScannedResult(result: ScannedWifiResult?) {
        _scannedResult.value = result
    }

    fun updateQrStyle(style: QrStyle) {
        _qrStyle.value = style
    }

    fun setEditingNetwork(network: WifiNetwork?) {
        _editingNetwork.value = network
    }

    fun connectInstantly(context: Context, wifiNet: WifiNetwork) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            showToast("Instant connect requires Android 10+")
            return
        }
        viewModelScope.launch {
            try {
                activeNetworkCallback?.let {
                    activeConnectivityManager?.unregisterNetworkCallback(it)
                }

                val specBuilder = WifiNetworkSpecifier.Builder().setSsid(wifiNet.ssid)
                when {
                    wifiNet.securityType == SecurityType.OPEN.name || wifiNet.password.isEmpty() -> {}
                    wifiNet.securityType == SecurityType.WEP.name -> {
                        showToast("WEP networks are not supported for instant connect")
                        return@launch
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            wifiNet.securityType == SecurityType.WPA3.name -> {
                        specBuilder.setWpa3Passphrase(wifiNet.password)
                    }
                    else -> specBuilder.setWpa2Passphrase(wifiNet.password)
                }
                if (wifiNet.isHidden) specBuilder.setIsHiddenSsid(true)

                val specifier = specBuilder.build()
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build()

                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                activeConnectivityManager = cm

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        viewModelScope.launch {
                            repo.updateLastConnected(wifiNet.id)
                            _connectMessage.value = "✓ Connected to ${wifiNet.ssid}"
                            showToast("Connected to ${wifiNet.ssid}")
                        }
                    }
                    override fun onUnavailable() {
                        viewModelScope.launch {
                            _connectMessage.value = "Could not reach ${wifiNet.ssid}"
                            showToast("Could not connect to ${wifiNet.ssid}")
                        }
                        activeNetworkCallback = null
                        activeConnectivityManager = null
                    }
                    override fun onLost(network: android.net.Network) {
                        viewModelScope.launch {
                            showToast("Lost connection to ${wifiNet.ssid}")
                        }
                        activeNetworkCallback = null
                        activeConnectivityManager = null
                    }
                }
                activeNetworkCallback = callback
                cm.requestNetwork(request, callback)

            } catch (e: Exception) {
                showToast("Connect failed: ${e.message}")
            }
        }
    }

    fun clearConnectMessage() {
        _connectMessage.value = null
    }

    fun backupNetworks(context: Context, folderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupMessage.value = "Creating backup…"
            val networks = repo.getAllNetworksList()
            val result = BackupManager.performBackup(context, folderUri, networks)
            _backupMessage.value = result.fold(
                onSuccess = { name -> "✓ Backup saved: $name (${networks.size} networks)" },
                onFailure = { e -> "Backup failed: ${e.message}" }
            )
        }
    }

    fun restoreNetworks(context: Context, fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupMessage.value = "Restoring…"
            val result = BackupManager.performRestore(context, fileUri)
            result.fold(
                onSuccess = { networks ->
                    var added = 0
                    var updated = 0
                    networks.forEach { n ->
                        val existing = repo.getBySsid(n.ssid)
                        if (existing != null) {
                            repo.update(n.copy(id = existing.id, savedAt = existing.savedAt))
                            updated++
                        } else {
                            repo.insert(n)
                            added++
                        }
                    }
                    _backupMessage.value = "✓ Restored: $added new networks, $updated updated"
                },
                onFailure = { e ->
                    _backupMessage.value = "Restore failed: ${e.message}"
                }
            )
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        activeNetworkCallback?.let {
            activeConnectivityManager?.unregisterNetworkCallback(it)
        }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
