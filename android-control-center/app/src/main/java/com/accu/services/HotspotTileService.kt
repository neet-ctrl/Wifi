package com.accu.services

import android.net.wifi.WifiManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.accu.connection.AccuConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Quick Settings tile for Hotspot toggle (BetterInternetTiles).
 */
@AndroidEntryPoint
class HotspotTileService : TileService() {

    @Inject lateinit var connectionManager: AccuConnectionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val isEnabled = withContext(Dispatchers.IO) { isHotspotEnabled() }
            withContext(Dispatchers.IO) {
                if (isEnabled) connectionManager.exec("cmd wifi stop-softap")
                else connectionManager.exec("cmd wifi start-softap")
            }
            delay(800)
            updateTile()
        }
    }

    private fun updateTile() {
        val isEnabled = isHotspotEnabled()
        qsTile?.apply {
            state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Hotspot"
            subtitle = if (isEnabled) "Active" else "Off"
            updateTile()
        }
    }

    private fun isHotspotEnabled(): Boolean {
        return try {
            val wifiManager = applicationContext.getSystemService(WifiManager::class.java)
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as? Boolean ?: false
        } catch (_: Exception) { false }
    }

    override fun onStopListening() { super.onStopListening(); scope.cancel() }
}
