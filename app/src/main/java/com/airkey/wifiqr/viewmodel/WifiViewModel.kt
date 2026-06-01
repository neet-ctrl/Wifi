package com.airkey.wifiqr.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airkey.wifiqr.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val repo = WifiRepository(db.wifiDao(), db.connectionEventDao(), db.geofenceConfigDao())

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

    private val _pdfExportMessage = MutableStateFlow<String?>(null)
    val pdfExportMessage: StateFlow<String?> = _pdfExportMessage.asStateFlow()

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
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repo.getBySsid(network.ssid)
            if (existing != null) {
                repo.update(network.copy(id = existing.id, savedAt = existing.savedAt))
            } else {
                repo.insert(network)
            }
        }
    }

    fun saveNetworkWithQr(context: Context, network: WifiNetwork, bitmap: Bitmap?) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repo.getBySsid(network.ssid)
            val id: Long = if (existing != null) {
                repo.update(network.copy(id = existing.id, savedAt = existing.savedAt))
                existing.id
            } else {
                repo.insert(network)
            }
            if (bitmap != null) {
                val path = QrImageStore.save(context, id, bitmap)
                repo.updateQrImagePath(id, path)
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
        if (!Settings.System.canWrite(context)) {
            viewModelScope.launch(Dispatchers.Main) {
                showToast("Need 'Modify System Settings' permission — opening Settings")
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                _connectMessage.value = "Grant 'Modify System Settings' permission, then try Connect again"
            }
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                activeNetworkCallback?.let { oldCallback ->
                    try { activeConnectivityManager?.unregisterNetworkCallback(oldCallback) } catch (_: Exception) {}
                }
                activeNetworkCallback = null
                activeConnectivityManager = null

                val specBuilder = WifiNetworkSpecifier.Builder().setSsid(wifiNet.ssid)
                when {
                    wifiNet.securityType == SecurityType.OPEN.name || wifiNet.password.isEmpty() -> {}
                    wifiNet.securityType == SecurityType.WEP.name -> {
                        showToast("WEP networks are not supported for instant connect")
                        return@launch
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wifiNet.securityType == SecurityType.WPA3.name -> {
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
                val mainHandler = Handler(Looper.getMainLooper())

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        viewModelScope.launch(Dispatchers.Main) {
                            repo.updateLastConnected(wifiNet.id)
                            viewModelScope.launch(Dispatchers.IO) {
                                repo.logConnectionEvent(wifiNet.id, wifiNet.ssid)
                            }
                            _connectMessage.value = "✓ Connected to ${wifiNet.ssid}"
                            showToast("Connected to ${wifiNet.ssid}")
                        }
                    }
                    override fun onUnavailable() {
                        viewModelScope.launch(Dispatchers.Main) {
                            _connectMessage.value = "Could not reach ${wifiNet.ssid}"
                            showToast("Could not connect to ${wifiNet.ssid}")
                            activeNetworkCallback = null
                            activeConnectivityManager = null
                        }
                    }
                    override fun onLost(network: android.net.Network) {
                        viewModelScope.launch(Dispatchers.Main) {
                            showToast("Lost connection to ${wifiNet.ssid}")
                            activeNetworkCallback = null
                            activeConnectivityManager = null
                        }
                    }
                }
                activeNetworkCallback = callback
                cm.requestNetwork(request, callback, mainHandler)
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
            val events = repo.getAllEventsList()
            val geofences = repo.getAllGeofencesList()
            val result = BackupManager.performBackup(context, folderUri, networks, events, geofences)
            _backupMessage.value = result.fold(
                onSuccess = { name -> "✓ Backup saved: $name (${networks.size} networks, ${events.size} events, ${geofences.size} geofences)" },
                onFailure = { e -> "Backup failed: ${e.message}" }
            )
        }
    }

    fun restoreNetworks(context: Context, fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupMessage.value = "Restoring…"
            val result = BackupManager.performRestore(context, fileUri)
            result.fold(
                onSuccess = { restoreResult ->
                    var added = 0; var updated = 0
                    // Build a map of old networkId → new networkId for remapping events/geofences
                    val idMap = mutableMapOf<Long, Long>()
                    restoreResult.networks.forEach { (n, imageBytes) ->
                        val existing = repo.getBySsid(n.ssid)
                        val oldId = n.id
                        val id: Long = if (existing != null) {
                            repo.update(n.copy(id = existing.id, savedAt = existing.savedAt))
                            updated++
                            existing.id
                        } else {
                            added++
                            repo.insert(n)
                        }
                        idMap[oldId] = id
                        if (imageBytes != null) {
                            val path = QrImageStore.saveFromBytes(context, id, imageBytes)
                            repo.updateQrImagePath(id, path)
                        }
                    }
                    // Restore connection events (remapping networkId)
                    restoreResult.connectionEvents.forEach { event ->
                        val remappedId = idMap[event.networkId] ?: event.networkId
                        repo.logConnectionEventFull(event.copy(id = 0, networkId = remappedId))
                    }
                    // Restore geofence configs (remapping networkId)
                    restoreResult.geofenceConfigs.forEach { geo ->
                        val remappedId = idMap[geo.networkId] ?: geo.networkId
                        repo.upsertGeofence(geo.copy(networkId = remappedId))
                    }
                    _backupMessage.value = "✓ Restored: $added new, $updated updated, ${restoreResult.connectionEvents.size} history events, ${restoreResult.geofenceConfigs.size} geofences"
                },
                onFailure = { e -> _backupMessage.value = "Restore failed: ${e.message}" }
            )
        }
    }

    fun clearBackupMessage() { _backupMessage.value = null }

    fun scheduleAutoBackup(context: Context, folderUri: Uri, intervalDays: Int) {
        val prefs = AutoBackupWorker.getPrefs(context)
        prefs.edit().putString(AutoBackupWorker.KEY_BACKUP_URI, folderUri.toString()).apply()
        AutoBackupWorker.schedule(context, intervalDays)
        _backupMessage.value = "✓ Auto-backup scheduled every ${if (intervalDays == 1) "day" else "$intervalDays days"}"
    }

    fun cancelAutoBackup(context: Context) {
        AutoBackupWorker.cancel(context)
        _backupMessage.value = "Auto-backup cancelled"
    }

    fun exportPdfBooklet(context: Context, folderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _pdfExportMessage.value = "Generating PDF…"
            val networks = repo.getAllNetworksList()
            if (networks.isEmpty()) {
                _pdfExportMessage.value = "No saved networks to export"
                return@launch
            }
            val result = PdfExportManager.exportBooklet(context, networks, folderUri)
            _pdfExportMessage.value = result.fold(
                onSuccess = { name -> "✓ PDF saved: $name (${networks.size} pages)" },
                onFailure = { e -> "PDF export failed: ${e.message}" }
            )
        }
    }

    fun clearPdfMessage() { _pdfExportMessage.value = null }

    suspend fun logConnectionEvent(networkId: Long, ssid: String): Long =
        withContext(Dispatchers.IO) { repo.logConnectionEvent(networkId, ssid) }

    fun updateConnectionEventSpeeds(
        eventId: Long,
        downloadMbps: Float,
        uploadMbps: Float,
        pingMs: Int,
        signalDbm: Int,
        frequencyMhz: Int,
        linkSpeedMbps: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val event = repo.getEventById(eventId) ?: return@launch
            repo.updateConnectionEvent(event.copy(
                downloadSpeedMbps = downloadMbps,
                uploadSpeedMbps = uploadMbps,
                pingMs = pingMs,
                signalDbm = signalDbm,
                frequencyMhz = frequencyMhz,
                linkSpeedMbps = linkSpeedMbps
            ))
        }
    }

    fun getEventsForNetwork(networkId: Long) = repo.getEventsForNetwork(networkId)

    fun upsertGeofence(config: GeofenceConfig) {
        viewModelScope.launch(Dispatchers.IO) { repo.upsertGeofence(config) }
    }

    fun deleteGeofence(config: GeofenceConfig) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteGeofence(config) }
    }

    suspend fun getGeofenceForNetwork(networkId: Long): GeofenceConfig? =
        withContext(Dispatchers.IO) { repo.getGeofenceForNetwork(networkId) }

    fun getAllGeofencesFlow() = repo.getAllGeofencesFlow()

    override fun onCleared() {
        super.onCleared()
        activeNetworkCallback?.let { activeConnectivityManager?.unregisterNetworkCallback(it) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
