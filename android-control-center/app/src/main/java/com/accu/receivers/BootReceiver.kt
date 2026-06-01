package com.accu.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.accu.connection.AccuConnectionManager
import com.accu.service.AccuSystemService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives BOOT_COMPLETED (and quick-boot / package-replaced variants) and:
 *  1. Auto-starts AccuSystemService if the user enabled "Boot autostart".
 *  2. Silently re-establishes the last WiFi ADB session so all features are
 *     available immediately after device reboot — no re-pairing needed.
 *
 * Reconnect runs in a short-lived coroutine (goAsync + SupervisorJob).
 * If it fails (device unreachable, ADB session expired) it stays silent —
 * the user will see the normal "Disconnected" state when they open ACCU.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var connectionManager: AccuConnectionManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Timber.i("BootReceiver: ${intent.action}")

        maybeAutoStartService(context)
        silentReconnect()
    }

    // ── 1. AccuSystemService autostart ────────────────────────────────────────

    private fun maybeAutoStartService(context: Context) {
        val prefs = context.getSharedPreferences(
            AccuSystemService.PREFS_SERVICE, Context.MODE_PRIVATE
        )
        val autostart = prefs.getBoolean(AccuSystemService.PREF_AUTOSTART, false)
        if (autostart) {
            Timber.i("Boot autostart enabled — starting AccuSystemService")
            try {
                context.startForegroundService(Intent(context, AccuSystemService::class.java))
            } catch (e: Exception) {
                Timber.e(e, "BootReceiver: failed to start AccuSystemService")
            }
        } else {
            Timber.d("Boot autostart disabled — skipping AccuSystemService start")
        }
    }

    // ── 2. Silent WiFi ADB reconnect ─────────────────────────────────────────

    /**
     * Uses goAsync() so the coroutine can run past onReceive's normal return,
     * but still finishes within the OS-allowed broadcast window (~10 s).
     *
     * Priority order inside [AccuConnectionManager.reconnect]:
     *   1. Root (LibSU)  — always succeeds if device is rooted
     *   2. dadb          — pure-Kotlin ADB protocol, uses the persisted RSA key
     *   3. System adb binary (rare on consumer devices)
     *
     * After a reboot the wireless ADB *session* port changes, but the RSA key
     * pairing persists on the target device.  dadb re-authenticates using the
     * stored key pair — no 6-digit code required.
     */
    private fun silentReconnect() {
        val ip = connectionManager.getLastConnectedIp()
        if (ip.isBlank()) {
            Timber.d("BootReceiver: no stored IP — skipping auto-reconnect")
            return
        }

        val pending = goAsync()
        val scope   = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                Timber.i("BootReceiver: attempting silent reconnect to $ip:${connectionManager.getLastConnectedPort()}")
                val ok = connectionManager.reconnect()
                if (ok) {
                    Timber.i("BootReceiver: auto-reconnect ✓ — privilege restored after reboot")
                } else {
                    Timber.d("BootReceiver: auto-reconnect failed — user will need to re-connect manually")
                }
            } catch (e: Exception) {
                Timber.w(e, "BootReceiver: auto-reconnect threw — staying disconnected")
            } finally {
                pending.finish()
            }
        }
    }
}
