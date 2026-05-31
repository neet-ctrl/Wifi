package com.accu.services

import android.net.ConnectivityManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.TelephonyManager
import com.accu.utils.ShizukuUtils
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Quick Settings tile for Mobile Data toggle (BetterInternetTiles).
 */
class MobileDataTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val shizukuUtils = ShizukuUtils()

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val cm = getSystemService(ConnectivityManager::class.java)
        val isEnabled = try {
            @Suppress("DEPRECATION")
            cm?.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        } catch (_: Exception) { false }

        scope.launch {
            withContext(Dispatchers.IO) {
                shizukuUtils.execShizuku("svc data ${if (isEnabled) "disable" else "enable"}")
            }
            delay(500)
            updateTile()
        }
    }

    private fun updateTile() {
        val telephonyManager = getSystemService(TelephonyManager::class.java)
        val isEnabled = telephonyManager?.dataState == TelephonyManager.DATA_CONNECTED ||
            telephonyManager?.dataState == TelephonyManager.DATA_ACTIVITY_IN

        qsTile?.apply {
            state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Mobile Data"
            subtitle = if (isEnabled) "Connected" else "Off"
            updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope.cancel()
    }
}
