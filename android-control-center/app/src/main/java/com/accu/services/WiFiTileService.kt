package com.accu.services

import android.graphics.drawable.Icon
import android.net.wifi.WifiManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.accu.R
import com.accu.utils.ShizukuUtils
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Quick Settings tile for Wi-Fi toggle (BetterInternetTiles).
 * Toggles Wi-Fi via ACCU without opening Settings.
 */
class WiFiTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val shizukuUtils = ShizukuUtils()

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val wifiManager = getSystemService(WifiManager::class.java)
        val currentlyEnabled = wifiManager?.isWifiEnabled ?: false
        scope.launch {
            withContext(Dispatchers.IO) {
                shizukuUtils.execShizuku("svc wifi ${if (currentlyEnabled) "disable" else "enable"}")
            }
            delay(500)
            updateTile()
        }
    }

    private fun updateTile() {
        val wifiManager = getSystemService(WifiManager::class.java)
        val isEnabled = wifiManager?.isWifiEnabled ?: false
        qsTile?.apply {
            state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Wi-Fi"
            subtitle = if (isEnabled) {
                wifiManager?.connectionInfo?.ssid?.removeSurrounding("\"") ?: "Connected"
            } else "Off"
            updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope.cancel()
    }
}
