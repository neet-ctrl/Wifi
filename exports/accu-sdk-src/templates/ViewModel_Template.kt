package com.yourcompany.yourapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.accu.sdk.AccuClient
import com.accu.sdk.AccuConnectionState
import com.accu.sdk.AccuConstants
import com.accu.sdk.AccuExecResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              ACCU SDK — ViewModel Template                               ║
 * ║                                                                          ║
 * ║  Copy this class into your project and rename it. It shows the          ║
 * ║  recommended pattern for using AccuClient in a Jetpack ViewModel.        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
class AccuViewModel(application: Application) : AndroidViewModel(application) {

    // ── AccuClient setup ─────────────────────────────────────────────────────

    /**
     * Use applicationContext so the client survives Activity rotations.
     * The connection is disconnected in onCleared().
     */
    private val accu = AccuClient(application.applicationContext)

    /**
     * Expose the connection state to your UI.
     * Collect this in your Composable or observe in an Activity/Fragment.
     */
    val accuState: StateFlow<AccuConnectionState> = accu.state

    // ── Example UI state ─────────────────────────────────────────────────────

    // Add your own StateFlow/MutableStateFlow fields here for your UI.

    // ── Lifecycle ────────────────────────────────────────────────────────────

    init {
        // Start connecting immediately when the ViewModel is created.
        accu.connect()
    }

    override fun onCleared() {
        super.onCleared()
        accu.disconnect()
    }

    // ── Permission ───────────────────────────────────────────────────────────

    /**
     * Call this when the user taps your "Connect to ACCU" button.
     * The ACCU permission dialog will appear automatically.
     *
     * Returns the permission result code (AccuConstants.PERMISSION_GRANTED, etc.)
     */
    fun requestAccuPermission(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val result = accu.requestPermission()
            onResult(result)
        }
    }

    // ── Example API calls ────────────────────────────────────────────────────

    /**
     * Example: run a shell command.
     * Always call ACCU APIs on Dispatchers.IO — they are synchronous
     * and will block the calling thread.
     */
    fun runShellCommand(command: String, onResult: (AccuExecResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                accu.exec(command)
            }
            onResult(result)
        }
    }

    /**
     * Example: disable a package.
     */
    fun disableApp(packageName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                accu.disablePackage(packageName)
            }
            onResult(success)
        }
    }

    /**
     * Example: write a secure setting.
     */
    fun writeSecureSetting(key: String, value: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                accu.writeSecureSetting(key, value)
            }
            onResult(success)
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Returns true if ACCU is connected AND permission is granted. */
    val isReady: Boolean
        get() = (accu.state.value as? AccuConnectionState.Connected)
            ?.isPermissionGranted ?: false
}
