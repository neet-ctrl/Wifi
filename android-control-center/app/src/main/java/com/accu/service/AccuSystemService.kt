package com.accu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.accu.MainActivity
import com.accu.api.IAccuPermissionCallback
import com.accu.connection.AccuConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                       ACCU SYSTEM SERVICE                                ║
 * ║                                                                          ║
 * ║  A foreground service that exposes IAccuService to other apps.           ║
 * ║  Runs in ACCU's own process; executes privileged ops via                 ║
 * ║  AccuConnectionManager (root or wireless ADB — no Shizuku needed).       ║
 * ║                                                                          ║
 * ║  Binding intent action: "com.accu.api.AccuSystemService"                 ║
 * ║  Package:               "com.accu.controlcenter"                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class AccuSystemService : Service() {

    companion object {
        const val ACTION_BIND           = "com.accu.api.AccuSystemService"
        const val NOTIFICATION_ID       = 8888
        const val CHANNEL_ID            = "accu_system_service"
        const val EXTRA_GRANT_PKG       = "grant_pkg"
        const val EXTRA_GRANT_LABEL     = "grant_label"
        const val ACTION_GRANT          = "com.accu.api.ACTION_GRANT"
        const val ACTION_DENY           = "com.accu.api.ACTION_DENY"

        const val PREFS_SERVICE         = "accu_service_prefs"
        const val PREF_AUTOSTART        = "accu_service_autostart"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _pendingRequests = MutableStateFlow<List<PendingPermRequest>>(emptyList())
        val pendingRequests: StateFlow<List<PendingPermRequest>> = _pendingRequests.asStateFlow()
    }

    data class PendingPermRequest(
        val packageName: String,
        val appLabel: String,
        val callback: IAccuPermissionCallback,
        val requestedAt: Long = System.currentTimeMillis(),
    )

    @Inject lateinit var permissionManager: AccuPermissionManager
    @Inject lateinit var connectionManager: AccuConnectionManager

    private lateinit var serviceImpl: AccuServiceImpl

    override fun onCreate() {
        super.onCreate()
        permissionManager.init(applicationContext)
        serviceImpl = AccuServiceImpl(
            context           = applicationContext,
            permissionManager = permissionManager,
            connectionManager = connectionManager,
            onPermissionRequest = ::handlePermissionRequest,
        )
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        _isRunning.value = true
        Timber.i("AccuSystemService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_GRANT -> {
                val pkg   = intent.getStringExtra(EXTRA_GRANT_PKG) ?: return START_STICKY
                val label = intent.getStringExtra(EXTRA_GRANT_LABEL) ?: pkg
                grantPending(pkg, label)
            }
            ACTION_DENY -> {
                val pkg = intent.getStringExtra(EXTRA_GRANT_PKG) ?: return START_STICKY
                denyPending(pkg)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.d("AccuSystemService bind request from intent: ${intent.action}")
        return serviceImpl
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        _pendingRequests.value = emptyList()
        Timber.i("AccuSystemService destroyed")
    }

    private fun handlePermissionRequest(
        callerPackage: String,
        callerLabel: String,
        callback: IAccuPermissionCallback,
    ) {
        when (permissionManager.checkPermission(callerPackage)) {
            ACCU_PERMISSION_GRANTED -> { callback.onPermissionResult(ACCU_PERMISSION_GRANTED); return }
            ACCU_PERMISSION_DENIED  -> { callback.onPermissionResult(ACCU_PERMISSION_DENIED);  return }
        }
        val request = PendingPermRequest(callerPackage, callerLabel, callback)
        _pendingRequests.value = _pendingRequests.value + request
        showPermissionNotification(callerPackage, callerLabel)
        launchPermissionActivity(callerPackage, callerLabel)
    }

    private fun grantPending(packageName: String, label: String) {
        val all = _pendingRequests.value
        val req = all.find { it.packageName == packageName } ?: return
        permissionManager.grant(applicationContext, packageName, ALL_SCOPES)
        try { req.callback.onPermissionResult(ACCU_PERMISSION_GRANTED) } catch (_: Exception) {}
        _pendingRequests.value = all.filter { it.packageName != packageName }
        updateNotification()
    }

    private fun denyPending(packageName: String) {
        val all = _pendingRequests.value
        val req = all.find { it.packageName == packageName } ?: return
        permissionManager.deny(applicationContext, packageName)
        try { req.callback.onPermissionResult(ACCU_PERMISSION_DENIED) } catch (_: Exception) {}
        _pendingRequests.value = all.filter { it.packageName != packageName }
        updateNotification()
    }

    fun grantFromActivity(packageName: String, scopes: Set<String>) {
        val all = _pendingRequests.value
        val req = all.find { it.packageName == packageName } ?: return
        permissionManager.grant(applicationContext, packageName, scopes)
        try { req.callback.onPermissionResult(ACCU_PERMISSION_GRANTED) } catch (_: Exception) {}
        _pendingRequests.value = all.filter { it.packageName != packageName }
        updateNotification()
    }

    fun denyFromActivity(packageName: String) = denyPending(packageName)

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "ACCU System Service", NotificationManager.IMPORTANCE_LOW)
            .apply {
                description = "Keeps the ACCU privilege API running for connected apps"
                setShowBadge(false)
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(pendingCount: Int = 0): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                action = "com.accu.OPEN_SERVICE_HUB"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val subtitle = if (pendingCount > 0)
            "$pendingCount app${if (pendingCount > 1) "s" else ""} requesting permission"
        else {
            val grantCount = permissionManager.grants.value.count { it.isGranted }
            "$grantCount app${if (grantCount != 1) "s" else ""} connected"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("ACCU System Service")
            .setContentText(subtitle)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showPermissionNotification(pkg: String, label: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val grantIntent = PendingIntent.getService(
            this, pkg.hashCode(),
            Intent(this, AccuSystemService::class.java).apply {
                action = ACTION_GRANT
                putExtra(EXTRA_GRANT_PKG, pkg)
                putExtra(EXTRA_GRANT_LABEL, label)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val denyIntent = PendingIntent.getService(
            this, pkg.hashCode() + 1,
            Intent(this, AccuSystemService::class.java).apply {
                action = ACTION_DENY
                putExtra(EXTRA_GRANT_PKG, pkg)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("$label wants ACCU access")
            .setContentText("Tap to review in ACCU, or use quick actions below.")
            .addAction(android.R.drawable.ic_menu_manage, "Grant Full Access", grantIntent)
            .addAction(android.R.drawable.ic_delete, "Deny", denyIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .build()
        nm.notify(pkg.hashCode(), notif)
        updateNotification()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(_pendingRequests.value.size))
    }

    private fun launchPermissionActivity(pkg: String, label: String) {
        val intent = Intent(this, AccuPermissionRequestActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_GRANT_PKG, pkg)
            putExtra(EXTRA_GRANT_LABEL, label)
        }
        startActivity(intent)
    }
}
