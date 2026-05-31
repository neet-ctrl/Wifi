package com.accu.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.accu.api.IAccuPermissionCallback
import com.accu.api.IAccuProcessCallback
import com.accu.api.IAccuService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                          AccuClient                                      ║
 * ║                   ACCU SDK — Primary Entry Point                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * AccuClient manages the lifecycle of your app's connection to
 * AccuSystemService. It wraps the raw AIDL binder, handles reconnection,
 * and exposes a clean Kotlin API.
 *
 * Quick-start:
 * ─────────────
 *   val accu = AccuClient(context)
 *
 *   // 1. Connect (call from onCreate / onStart)
 *   accu.connect()
 *
 *   // 2. Observe state
 *   accu.state.collect { state ->
 *       when (state) {
 *           is AccuConnectionState.Connected -> { /* ready */ }
 *           is AccuConnectionState.Error     -> { /* show error */ }
 *           else -> { /* waiting */ }
 *       }
 *   }
 *
 *   // 3. Request permission (first time only)
 *   accu.requestPermission()
 *
 *   // 4. Call APIs
 *   val result = accu.exec("id")  // requires SHELL scope
 *
 *   // 5. Disconnect (call from onStop / onDestroy)
 *   accu.disconnect()
 *
 * Thread safety:
 *   connect() / disconnect() must be called from the main thread.
 *   All API calls are synchronous and BLOCK the calling thread.
 *   Wrap them in Dispatchers.IO or a background coroutine.
 *
 * ViewModel usage: see ViewModel_Template.kt in the templates/ folder.
 */
class AccuClient(private val context: Context) {

    // ── State ────────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow<AccuConnectionState>(AccuConnectionState.Idle)

    /**
     * Observable connection state. Collect this in your UI layer.
     * See AccuConnectionState for all possible values.
     */
    val state: StateFlow<AccuConnectionState> = _state.asStateFlow()

    /** Raw binder — null when not connected. Use the typed API methods instead. */
    private var service: IAccuService? = null

    // ── ServiceConnection ────────────────────────────────────────────────────

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val svc = IAccuService.Stub.asInterface(binder)
            service = svc
            val permCode    = try { svc.checkPermission() } catch (_: Exception) { AccuConstants.PERMISSION_NOT_YET_REQUESTED }
            val version     = try { svc.getVersion() } catch (_: Exception) { -1 }
            val accuVersion = try { svc.getAccuVersion() } catch (_: Exception) { "unknown" }
            _state.value = AccuConnectionState.Connected(
                permissionCode  = permCode,
                serviceVersion  = version,
                accuVersion     = accuVersion,
            )
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            _state.value = AccuConnectionState.Disconnected
            // Auto-reconnect
            connect()
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Bind to AccuSystemService. Call from Activity.onStart() or
     * ViewModel.init block (with applicationContext).
     *
     * If ACCU is not installed, state becomes AccuConnectionState.Error.
     */
    fun connect() {
        if (!isAccuInstalled()) {
            _state.value = AccuConnectionState.Error(
                "ACCU (${AccuConstants.ACCU_PACKAGE}) is not installed."
            )
            return
        }
        _state.value = AccuConnectionState.Connecting
        val intent = Intent(AccuConstants.SERVICE_ACTION).apply {
            `package` = AccuConstants.ACCU_PACKAGE
        }
        val bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            false
        }
        if (!bound) {
            _state.value = AccuConnectionState.Error(
                "bindService() returned false. Is AccuSystemService running? " +
                "Ask the user to open ACCU → System Service → Enable."
            )
        }
    }

    /**
     * Unbind from AccuSystemService. Call from Activity.onStop() or
     * when your ViewModel is cleared (viewModelScope.onCleared).
     */
    fun disconnect() {
        try { context.unbindService(connection) } catch (_: Exception) {}
        service = null
        _state.value = AccuConnectionState.Idle
    }

    // ── Permission ───────────────────────────────────────────────────────────

    /**
     * Ask ACCU to show its permission-grant dialog to the user.
     *
     * This is a suspending function — it suspends until the user responds.
     * Returns one of AccuConstants.PERMISSION_* codes.
     *
     * You MUST call this before any privileged API. After PERMISSION_GRANTED
     * is returned, you do NOT need to call it again — ACCU remembers.
     *
     * Usage:
     *   val code = accu.requestPermission()
     *   if (code == AccuConstants.PERMISSION_GRANTED) { ... }
     */
    suspend fun requestPermission(): Int = suspendCancellableCoroutine { cont ->
        val svc = service ?: run {
            cont.resume(AccuConstants.PERMISSION_SERVICE_UNAVAILABLE)
            return@suspendCancellableCoroutine
        }
        try {
            svc.requestPermission(object : IAccuPermissionCallback.Stub() {
                override fun onPermissionResult(result: Int) {
                    // Update state if permission was granted
                    val current = _state.value
                    if (current is AccuConnectionState.Connected) {
                        _state.value = current.copy(permissionCode = result)
                    }
                    if (cont.isActive) cont.resume(result)
                }
            })
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(AccuConstants.PERMISSION_SERVICE_UNAVAILABLE)
        }
    }

    /**
     * Check the current permission status synchronously.
     * Does NOT show any dialog — call requestPermission() for that.
     */
    fun checkPermission(): Int =
        try { service?.checkPermission() ?: AccuConstants.PERMISSION_SERVICE_UNAVAILABLE }
        catch (_: Exception) { AccuConstants.PERMISSION_SERVICE_UNAVAILABLE }

    /**
     * Check if your app has a specific scope granted.
     * Returns false if not connected or scope not granted.
     */
    fun hasScope(scope: String): Boolean =
        try { service?.hasScope(scope) ?: false }
        catch (_: Exception) { false }

    /**
     * Revoke your own permission. After this, the user must grant again.
     */
    fun revokeSelf() {
        try { service?.revokeSelf() } catch (_: Exception) {}
        val current = _state.value
        if (current is AccuConnectionState.Connected) {
            _state.value = current.copy(permissionCode = AccuConstants.PERMISSION_NOT_YET_REQUESTED)
        }
    }

    // ── Shell ────────────────────────────────────────────────────────────────

    /**
     * Execute a shell command synchronously.
     * Requires scope: AccuScopes.SHELL
     *
     * @param command Shell command string (passed to sh -c).
     * @return AccuExecResult with stdout, stderr, and exit code.
     * @throws AccuNotConnectedException if not connected.
     * @throws AccuScopeDeniedException if SHELL scope not granted.
     */
    fun exec(command: String): AccuExecResult {
        val svc = requireService()
        val raw = svc.exec(command)
        return AccuExecResult(
            stdout   = raw.getOrElse(0) { "" },
            stderr   = raw.getOrElse(1) { "" },
            exitCode = raw.getOrElse(2) { "-1" }.toIntOrNull() ?: -1,
        )
    }

    /**
     * Execute a command and stream output asynchronously.
     * Requires scope: AccuScopes.SHELL
     *
     * Callbacks are delivered on a background thread — post to your
     * own handler or use withContext(Dispatchers.Main) inside them.
     */
    fun execAsync(
        command: String,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onExit:   (Int)    -> Unit,
    ) {
        val svc = requireService()
        svc.execAsync(command, object : IAccuProcessCallback.Stub() {
            override fun onStdoutLine(line: String) { onStdout(line) }
            override fun onStderrLine(line: String) { onStderr(line) }
            override fun onExit(exitCode: Int)      { onExit(exitCode) }
        })
    }

    /**
     * Execute a command and return combined output as a single String.
     * Requires scope: AccuScopes.SHELL
     */
    fun execAndGetOutput(command: String): String =
        requireService().execAndGetOutput(command)

    // ── Package Manager ──────────────────────────────────────────────────────

    /** Install an APK from an absolute file path. Requires PACKAGE_MANAGE. */
    fun installApk(apkPath: String, installerPackage: String? = null): Boolean =
        requireService().installApk(apkPath, installerPackage)

    /** Uninstall a package (removes data). Requires PACKAGE_MANAGE. */
    fun uninstallPackage(packageName: String): Boolean =
        requireService().uninstallPackage(packageName)

    /** Uninstall keeping data (pm uninstall -k). Requires PACKAGE_MANAGE. */
    fun uninstallKeepData(packageName: String): Boolean =
        requireService().uninstallKeepData(packageName)

    /** Re-enable a disabled package. Requires PACKAGE_MANAGE. */
    fun enablePackage(packageName: String): Boolean =
        requireService().enablePackage(packageName)

    /** Disable a package (pm disable-user). Requires PACKAGE_MANAGE. */
    fun disablePackage(packageName: String): Boolean =
        requireService().disablePackage(packageName)

    /** Hide a package (soft-uninstall, no data loss). Requires PACKAGE_MANAGE. */
    fun hidePackage(packageName: String): Boolean =
        requireService().hidePackage(packageName)

    /** Unhide a previously hidden package. Requires PACKAGE_MANAGE. */
    fun unhidePackage(packageName: String): Boolean =
        requireService().unhidePackage(packageName)

    /** Suspend a package (greyed out, can't open). Requires PACKAGE_MANAGE. */
    fun suspendPackage(packageName: String): Boolean =
        requireService().suspendPackage(packageName)

    /** Unsuspend a package. Requires PACKAGE_MANAGE. */
    fun unsuspendPackage(packageName: String): Boolean =
        requireService().unsuspendPackage(packageName)

    /** Clear all data for a package. Requires PACKAGE_MANAGE. */
    fun clearPackageData(packageName: String): Boolean =
        requireService().clearPackageData(packageName)

    /** Enable a specific component. Requires PACKAGE_MANAGE. */
    fun enableComponent(packageName: String, componentName: String): Boolean =
        requireService().enableComponent(packageName, componentName)

    /** Disable a specific component. Requires PACKAGE_MANAGE. */
    fun disableComponent(packageName: String, componentName: String): Boolean =
        requireService().disableComponent(packageName, componentName)

    /** Force-stop a package. Requires PACKAGE_MANAGE. */
    fun forceStop(packageName: String): Boolean =
        requireService().forceStop(packageName)

    // ── Runtime Permissions ──────────────────────────────────────────────────

    /** Grant a runtime permission. Requires PERMISSIONS scope. */
    fun grantPermission(packageName: String, permission: String): Boolean =
        requireService().grantPermission(packageName, permission)

    /** Revoke a runtime permission. Requires PERMISSIONS scope. */
    fun revokePermission(packageName: String, permission: String): Boolean =
        requireService().revokePermission(packageName, permission)

    /**
     * Set an AppOp mode. Requires PERMISSIONS scope.
     * @param op  e.g. "CAMERA", "READ_CONTACTS", "RECORD_AUDIO"
     * @param mode "allow" | "deny" | "ignore" | "default"
     */
    fun setAppOp(packageName: String, op: String, mode: String): Boolean =
        requireService().setAppOp(packageName, op, mode)

    /** Get the current AppOp mode. Requires PERMISSIONS scope. */
    fun getAppOp(packageName: String, op: String): String =
        requireService().getAppOp(packageName, op)

    // ── Locale ───────────────────────────────────────────────────────────────

    /**
     * Set per-app locale. Requires LOCALE scope.
     * @param locale BCP 47 tag e.g. "en-US", "ja-JP". Pass "" to reset to system.
     */
    fun setApplicationLocale(packageName: String, locale: String): Boolean =
        requireService().setApplicationLocale(packageName, locale)

    // ── Settings ─────────────────────────────────────────────────────────────

    fun writeSecureSetting(name: String, value: String): Boolean =
        requireService().writeSecureSetting(name, value)

    fun readSecureSetting(name: String): String =
        requireService().readSecureSetting(name)

    fun writeGlobalSetting(name: String, value: String): Boolean =
        requireService().writeGlobalSetting(name, value)

    fun readGlobalSetting(name: String): String =
        requireService().readGlobalSetting(name)

    fun writeSystemSetting(name: String, value: String): Boolean =
        requireService().writeSystemSetting(name, value)

    fun readSystemSetting(name: String): String =
        requireService().readSystemSetting(name)

    // ── Identity ─────────────────────────────────────────────────────────────

    /** Returns true if AccuSystemService is alive. */
    fun ping(): Boolean = try { service?.ping() ?: false } catch (_: Exception) { false }

    /** ACCU IPC protocol version (currently 1). */
    fun getVersion(): Int = try { service?.getVersion() ?: -1 } catch (_: Exception) { -1 }

    /** UID of the ACCU service process. */
    fun getUid(): Int = try { service?.getUid() ?: -1 } catch (_: Exception) { -1 }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun requireService(): IAccuService =
        service ?: throw AccuNotConnectedException()

    private fun isAccuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(AccuConstants.ACCU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }
}

/** Result type returned by AccuClient.exec(). */
data class AccuExecResult(
    val stdout:   String,
    val stderr:   String,
    val exitCode: Int,
) {
    val isSuccess: Boolean get() = exitCode == 0
    val output:    String  get() = buildString {
        if (stdout.isNotBlank()) append(stdout.trim())
        if (stderr.isNotBlank()) { if (isNotEmpty()) append("\n"); append(stderr.trim()) }
    }
}
