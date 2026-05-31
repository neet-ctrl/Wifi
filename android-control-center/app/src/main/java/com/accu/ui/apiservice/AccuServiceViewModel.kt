package com.accu.ui.apiservice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.service.AccuClientGrant
import com.accu.service.AccuPermissionManager
import com.accu.service.AccuSystemService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AccuServiceUiState(
    val isServiceRunning: Boolean = false,
    val connectedApps: List<AccuClientGrant> = emptyList(),
    val pendingRequests: List<AccuSystemService.PendingPermRequest> = emptyList(),
    val totalApiCalls: Long = 0L,
    val selectedTab: ServiceTab = ServiceTab.APPS,
)

enum class ServiceTab { APPS, PENDING, DOCS }

@HiltViewModel
class AccuServiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: AccuPermissionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AccuServiceUiState())
    val state: StateFlow<AccuServiceUiState> = _state.asStateFlow()

    init {
        permissionManager.init(context)

        // Observe service running state
        viewModelScope.launch {
            AccuSystemService.isRunning.collect { running ->
                _state.update { it.copy(isServiceRunning = running) }
            }
        }
        // Observe pending requests
        viewModelScope.launch {
            AccuSystemService.pendingRequests.collect { pending ->
                _state.update { it.copy(pendingRequests = pending) }
            }
        }
        // Observe granted apps
        viewModelScope.launch {
            permissionManager.grants.collect { grants ->
                _state.update {
                    it.copy(
                        connectedApps = grants.sortedByDescending { g -> g.lastUsedAt },
                        totalApiCalls = grants.sumOf { g -> g.callCount },
                    )
                }
            }
        }
    }

    // ── Service start/stop ────────────────────────────────────────────────────

    fun startService() {
        try {
            val intent = Intent(context, AccuSystemService::class.java)
            context.startForegroundService(intent)
            Timber.i("AccuSystemService start requested")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start AccuSystemService")
        }
    }

    fun stopService() {
        try {
            context.stopService(Intent(context, AccuSystemService::class.java))
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop AccuSystemService")
        }
    }

    // ── Permission management ─────────────────────────────────────────────────

    fun revokeApp(packageName: String) {
        permissionManager.revoke(packageName)
    }

    fun deleteApp(packageName: String) {
        permissionManager.delete(packageName)
    }

    fun grantPending(packageName: String) {
        startService(Intent(context, AccuSystemService::class.java).apply {
            action = AccuSystemService.ACTION_GRANT
            putExtra(AccuSystemService.EXTRA_GRANT_PKG, packageName)
        })
    }

    fun denyPending(packageName: String) {
        startService(Intent(context, AccuSystemService::class.java).apply {
            action = AccuSystemService.ACTION_DENY
            putExtra(AccuSystemService.EXTRA_GRANT_PKG, packageName)
        })
    }

    // ── Tab selection ─────────────────────────────────────────────────────────

    fun selectTab(tab: ServiceTab) = _state.update { it.copy(selectedTab = tab) }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun startService(intent: Intent) {
        try { context.startService(intent) } catch (e: Exception) { Timber.e(e) }
    }
}
