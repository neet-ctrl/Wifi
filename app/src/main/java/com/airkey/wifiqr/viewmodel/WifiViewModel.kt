package com.airkey.wifiqr.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
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

    val categories = listOf("All", "Home", "Work", "Travel", "Public", "Guest", "General")

    init {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _selectedCategory
            ) { query, category -> Pair(query, category) }
                .flatMapLatest { (query, category) ->
                    when {
                        query.isNotEmpty() -> repo.searchNetworks(query)
                        category != "All" -> repo.getByCategory(category)
                        else -> repo.allNetworks
                    }
                }
                .collect { networks ->
                    _uiState.update { it.copy(networks = networks, networkCount = networks.size) }
                }
        }
    }

    fun onSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onQrScanned(raw: String) {
        val result = parseWifiQrCode(raw)
        _scannedResult.value = result
    }

    fun clearScannedResult() {
        _scannedResult.value = null
    }

    fun saveNetwork(network: WifiNetwork) {
        viewModelScope.launch {
            val existing = repo.getBySsid(network.ssid)
            if (existing != null) {
                repo.update(network.copy(id = existing.id))
                showToast("Network updated!")
            } else {
                repo.insert(network)
                showToast("Network saved!")
            }
        }
    }

    fun deleteNetwork(network: WifiNetwork) {
        viewModelScope.launch {
            repo.delete(network)
            showToast("Network deleted")
        }
    }

    fun toggleFavorite(network: WifiNetwork) {
        viewModelScope.launch {
            repo.setFavorite(network.id, !network.isFavorite)
        }
    }

    fun updateQrStyle(style: QrStyle) {
        _qrStyle.value = style
    }

    fun setEditingNetwork(network: WifiNetwork?) {
        _editingNetwork.value = network
    }

    fun connectToWifi(context: Context, network: WifiNetwork) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val suggestion = WifiNetworkSuggestion.Builder()
                        .setSsid(network.ssid)
                        .apply {
                            if (network.password.isNotEmpty()) {
                                when (network.securityType) {
                                    SecurityType.WEP.name -> setWpa2Passphrase(network.password)
                                    SecurityType.WPA3.name -> setWpa3Passphrase(network.password)
                                    SecurityType.OPEN.name -> { /* no password */ }
                                    else -> setWpa2Passphrase(network.password)
                                }
                            }
                        }
                        .setIsHiddenSsid(network.isHidden)
                        .build()
                    wifiManager.addNetworkSuggestions(listOf(suggestion))
                    repo.updateLastConnected(network.id)
                    showToast("Connection suggested for ${network.ssid}")
                } else {
                    showToast("Auto-connect available on Android 10+")
                }
            } catch (e: Exception) {
                showToast("Could not connect: ${e.message}")
            }
        }
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

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
