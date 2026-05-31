package com.accu.services

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.accu.connection.AccuConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FreezeAllTileService : TileService() {

    @Inject lateinit var connectionManager: AccuConnectionManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val prefs = getSharedPreferences("accu_freeze_prefs", Context.MODE_PRIVATE)
            val frozen = prefs.getStringSet("frozen_packages", emptySet()) ?: emptySet()
            if (frozen.isEmpty()) return@launch
            if (!connectionManager.isPrivilegeAvailable()) return@launch
            frozen.forEach { pkg ->
                try { connectionManager.exec("pm suspend $pkg") } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Freeze All"
        tile.updateTile()
    }
}
