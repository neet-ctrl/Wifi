package com.accu.connection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.accu.MainActivity
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    ACCU CONNECTION MANAGER                               ║
 * ║                                                                          ║
 * ║  Single global privilege source for all ACCU features.                  ║
 * ║  ACCU is its own self-sufficient privilege broker.                       ║
 * ║                                                                          ║
 * ║  Privilege priority:                                                     ║
 * ║    1. Root (LibSU)        — preferred, no setup needed on rooted devices ║
 * ║    2. Wireless ADB        — auto-discovered via mDNS, standard ADB flow  ║
 * ║    3. Plain shell         — unprivileged fallback                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Singleton
class AccuConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "AccuConnectionManager"
        private const val PREFS_NAME = "accu_connection_prefs"
        private const val KEY_LAST_IP = "last_adb_ip"
        private const val KEY_LAST_PORT = "last_adb_port"

        // Android Wireless Debugging mDNS service types
        private const val MDNS_PAIRING = "_adb-tls-pairing._tcp"
        private const val MDNS_CONNECT = "_adb-tls-connect._tcp"

        // Notification
        const val CHANNEL_ID = "accu_connection"
        const val CHANNEL_NAME = "ACCU Privileged Connection"
        const val NOTIF_ID_PAIRING = 7001
        const val NOTIF_ID_CONNECTED = 7002
        const val NOTIF_ID_DISCONNECTED = 7003
    }

    enum class ConnectionState {
        /** No privilege at all — limited functionality */
        DISCONNECTED,
        /** Listening for the Android Wireless Debugging pairing mDNS service */
        DISCOVERING,
        /** Pairing service found; waiting for user to enter the 6-digit code */
        AWAITING_CODE,
        /** Executing `adb pair` + `adb connect` */
        CONNECTING,
        /** ADB wireless session active */
        CONNECTED_WIRELESS,
        /** LibSU root available — full privilege without ADB */
        CONNECTED_ROOT,
    }

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** Discovered pairing host + port (auto-filled via mDNS, no manual entry) */
    private var pairingHost: String = ""
    private var pairingPort: Int = 0

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val nm: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private var pairingListener: NsdManager.DiscoveryListener? = null
    private var connectListener: NsdManager.DiscoveryListener? = null

    // ─── Public API ────────────────────────────────────────────────────────────

    /** true if root or wireless ADB is available */
    fun isPrivilegeAvailable(): Boolean = when (_state.value) {
        ConnectionState.CONNECTED_ROOT, ConnectionState.CONNECTED_WIRELESS -> true
        else -> Shell.getShell().isRoot.also { if (it) _state.value = ConnectionState.CONNECTED_ROOT }
    }

    fun getDeviceIp(): String = try {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.filter { !it.isLoopbackAddress && it is java.net.Inet4Address }
            ?.firstOrNull()?.hostAddress ?: ""
    } catch (_: Exception) { "" }

    fun getLastConnectedIp(): String = prefs.getString(KEY_LAST_IP, "") ?: ""
    fun getLastConnectedPort(): Int = prefs.getInt(KEY_LAST_PORT, 5555)

    /**
     * Execute a shell command using the best available privilege source.
     * Priority: root → wireless ADB → plain shell
     */
    suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            when {
                Shell.getShell().isRoot -> execRoot(command)
                _state.value == ConnectionState.CONNECTED_WIRELESS -> execPlainShell(command)
                else -> execPlainShell(command)
            }
        } catch (e: Exception) {
            Timber.e(e, "AccuConnection exec failed: $command")
            ShellResult("", e.message ?: "error", -1)
        }
    }

    fun execRoot(command: String): ShellResult = try {
        val result = Shell.cmd(command).exec()
        ShellResult(
            output = result.out.joinToString("\n"),
            error = result.err.joinToString("\n"),
            exitCode = if (result.isSuccess) 0 else 1,
        )
    } catch (e: Exception) {
        ShellResult("", e.message ?: "error", -1)
    }

    fun execPlainShell(command: String): ShellResult = try {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        ShellResult(stdout, stderr, exit)
    } catch (e: Exception) {
        ShellResult("", e.message ?: "error", -1)
    }

    // ─── Pairing flow (wireless ADB setup) ────────────────────────────────────

    /**
     * Step 1 — Start auto-discovery of Android Wireless Debugging pairing service.
     * Shows a notification once the device broadcasts its pairing port via mDNS.
     * User only needs to enter the 6-digit code shown on their Developer Settings screen.
     */
    fun startPairingDiscovery() {
        if (_state.value == ConnectionState.DISCOVERING || _state.value == ConnectionState.AWAITING_CODE) return
        Timber.i("AccuConnection: starting mDNS discovery for $MDNS_PAIRING")
        _state.value = ConnectionState.DISCOVERING

        ensureNotificationChannel()

        pairingListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(type: String, code: Int) {
                Timber.w("mDNS discovery start failed: $code")
                _state.value = ConnectionState.DISCONNECTED
            }
            override fun onStopDiscoveryFailed(type: String, code: Int) {}
            override fun onDiscoveryStarted(type: String) {
                Timber.d("mDNS discovery started for $type")
            }
            override fun onDiscoveryStopped(type: String) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                Timber.i("mDNS pairing service found: ${info.serviceName}")
                nm.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(s: NsdServiceInfo, code: Int) {
                        Timber.w("mDNS resolve failed: $code")
                    }
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: getDeviceIp()
                        val port = resolved.port
                        Timber.i("Pairing service resolved: $host:$port")
                        pairingHost = host
                        pairingPort = port
                        _state.value = ConnectionState.AWAITING_CODE
                        showPairingCodeNotification(host, port)
                    }
                })
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                Timber.d("mDNS pairing service lost")
                if (_state.value == ConnectionState.AWAITING_CODE) {
                    _state.value = ConnectionState.DISCOVERING
                }
            }
        }

        try {
            nm.discoverServices(MDNS_PAIRING, NsdManager.PROTOCOL_DNS_SD, pairingListener)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start mDNS discovery")
            _state.value = ConnectionState.DISCONNECTED
        }
    }

    fun stopPairingDiscovery() {
        pairingListener?.let {
            try { nm.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        pairingListener = null
        if (_state.value == ConnectionState.DISCOVERING || _state.value == ConnectionState.AWAITING_CODE) {
            _state.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Step 2 — Called when user enters the 6-digit code shown in Wireless Debugging.
     * ACCU auto-pairs using the IP/port discovered via mDNS — no manual entry needed.
     */
    suspend fun completePairing(code: String): Boolean = withContext(Dispatchers.IO) {
        val host = pairingHost.ifBlank { getDeviceIp() }
        val port = if (pairingPort > 0) pairingPort else return@withContext false
        Timber.i("AccuConnection: pairing with $host:$port using code $code")
        _state.value = ConnectionState.CONNECTING

        val pairResult = execPlainShell("adb pair $host:$port $code")
        val pairOk = pairResult.output.contains("Successfully", ignoreCase = true) || pairResult.exitCode == 0

        if (pairOk) {
            // Now discover and connect to the ongoing ADB service
            val connectOk = connectToWirelessAdb(host)
            if (connectOk) {
                _state.value = ConnectionState.CONNECTED_WIRELESS
                prefs.edit().putString(KEY_LAST_IP, host).putInt(KEY_LAST_PORT, 5555).apply()
                showConnectedNotification(host)
                stopPairingDiscovery()
                return@withContext true
            }
        }

        Timber.w("AccuConnection: pairing failed — ${pairResult.combinedOutput}")
        _state.value = ConnectionState.AWAITING_CODE
        false
    }

    /** Reconnect to the last known wireless ADB session (e.g. after app restart) */
    suspend fun reconnect(): Boolean = withContext(Dispatchers.IO) {
        val ip = getLastConnectedIp()
        if (ip.isBlank()) return@withContext false
        val port = getLastConnectedPort()
        val result = execPlainShell("adb connect $ip:$port")
        val ok = result.combinedOutput.contains("connected", ignoreCase = true)
        if (ok) _state.value = ConnectionState.CONNECTED_WIRELESS
        ok
    }

    fun disconnect() {
        val ip = getLastConnectedIp()
        if (ip.isNotBlank()) {
            try { Runtime.getRuntime().exec(arrayOf("sh", "-c", "adb disconnect $ip")) } catch (_: Exception) {}
        }
        prefs.edit().remove(KEY_LAST_IP).remove(KEY_LAST_PORT).apply()
        _state.value = ConnectionState.DISCONNECTED
        showDisconnectedNotification()
    }

    fun checkAndUpdateState() {
        _state.value = when {
            Shell.getShell().isRoot -> ConnectionState.CONNECTED_ROOT
            getLastConnectedIp().isNotBlank() -> ConnectionState.CONNECTED_WIRELESS
            else -> ConnectionState.DISCONNECTED
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private fun connectToWirelessAdb(host: String): Boolean {
        val result = execPlainShell("adb connect $host:5555")
        return result.combinedOutput.contains("connected", ignoreCase = true)
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "ACCU wireless ADB connection status and pairing codes"
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun showPairingCodeNotification(host: String, port: Int) {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                action = "com.accu.OPEN_CONNECTION"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Wireless Debugging Detected")
            .setContentText("Open ACCU to enter the 6-digit pairing code ($host:$port auto-detected)")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("Pairing service found at $host:$port.\n\nOpen ACCU and enter the 6-digit code shown in:\nSettings → Developer Options → Wireless Debugging"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID_PAIRING, notif)
    }

    private fun showConnectedNotification(ip: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_PAIRING)
        val notif = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("ACCU Connected")
            .setContentText("Wireless ADB active — all privileged features enabled ($ip)")
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_CONNECTED, notif)
    }

    private fun showDisconnectedNotification() {
        val notif = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("ACCU Disconnected")
            .setContentText("Privileged access lost. Open ACCU to reconnect.")
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID_DISCONNECTED, notif)
    }
}

data class ShellResult(
    val output: String,
    val error: String,
    val exitCode: Int,
) {
    val isSuccess get() = exitCode == 0
    val combinedOutput get() = buildString {
        if (output.isNotBlank()) append(output)
        if (error.isNotBlank()) { if (isNotEmpty()) append("\n"); append("[ERR] $error") }
    }
}
