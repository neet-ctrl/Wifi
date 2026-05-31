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

class AccuClient(private val context: Context) {

    private val _state = MutableStateFlow<AccuConnectionState>(AccuConnectionState.Idle)
    val state: StateFlow<AccuConnectionState> = _state.asStateFlow()

    private var service: IAccuService? = null

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
            connect()
        }
    }

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

    fun disconnect() {
        try { context.unbindService(connection) } catch (_: Exception) {}
        service = null
        _state.value = AccuConnectionState.Idle
    }

    suspend fun requestPermission(): Int = suspendCancellableCoroutine { cont ->
        val svc = service ?: run {
            cont.resume(AccuConstants.PERMISSION_SERVICE_UNAVAILABLE)
            return@suspendCancellableCoroutine
        }
        try {
            svc.requestPermission(object : IAccuPermissionCallback.Stub() {
                override fun onPermissionResult(result: Int) {
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

    fun checkPermission(): Int =
        try { service?.checkPermission() ?: AccuConstants.PERMISSION_SERVICE_UNAVAILABLE }
        catch (_: Exception) { AccuConstants.PERMISSION_SERVICE_UNAVAILABLE }

    fun hasScope(scope: String): Boolean =
        try { service?.hasScope(scope) ?: false }
        catch (_: Exception) { false }

    fun revokeSelf() {
        try { service?.revokeSelf() } catch (_: Exception) {}
        val current = _state.value
        if (current is AccuConnectionState.Connected) {
            _state.value = current.copy(permissionCode = AccuConstants.PERMISSION_NOT_YET_REQUESTED)
        }
    }

    fun exec(command: String): AccuExecResult {
        val svc = requireService()
        val raw = svc.exec(command)
        return AccuExecResult(
            stdout   = raw.getOrElse(0) { "" },
            stderr   = raw.getOrElse(1) { "" },
            exitCode = raw.getOrElse(2) { "-1" }.toIntOrNull() ?: -1,
        )
    }

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

    fun execAndGetOutput(command: String): String =
        requireService().execAndGetOutput(command)

    fun installApk(apkPath: String, installerPackage: String? = null): Boolean =
        requireService().installApk(apkPath, installerPackage)

    fun uninstallPackage(packageName: String): Boolean =
        requireService().uninstallPackage(packageName)

    fun uninstallKeepData(packageName: String): Boolean =
        requireService().uninstallKeepData(packageName)

    fun enablePackage(packageName: String): Boolean =
        requireService().enablePackage(packageName)

    fun disablePackage(packageName: String): Boolean =
        requireService().disablePackage(packageName)

    fun hidePackage(packageName: String): Boolean =
        requireService().hidePackage(packageName)

    fun unhidePackage(packageName: String): Boolean =
        requireService().unhidePackage(packageName)

    fun suspendPackage(packageName: String): Boolean =
        requireService().suspendPackage(packageName)

    fun unsuspendPackage(packageName: String): Boolean =
        requireService().unsuspendPackage(packageName)

    fun clearPackageData(packageName: String): Boolean =
        requireService().clearPackageData(packageName)

    fun enableComponent(packageName: String, componentName: String): Boolean =
        requireService().enableComponent(packageName, componentName)

    fun disableComponent(packageName: String, componentName: String): Boolean =
        requireService().disableComponent(packageName, componentName)

    fun forceStop(packageName: String): Boolean =
        requireService().forceStop(packageName)

    fun grantPermission(packageName: String, permission: String): Boolean =
        requireService().grantPermission(packageName, permission)

    fun revokePermission(packageName: String, permission: String): Boolean =
        requireService().revokePermission(packageName, permission)

    fun setAppOp(packageName: String, op: String, mode: String): Boolean =
        requireService().setAppOp(packageName, op, mode)

    fun getAppOp(packageName: String, op: String): String =
        requireService().getAppOp(packageName, op)

    fun setApplicationLocale(packageName: String, locale: String): Boolean =
        requireService().setApplicationLocale(packageName, locale)

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

    fun ping(): Boolean = try { service?.ping() ?: false } catch (_: Exception) { false }

    fun getVersion(): Int = try { service?.getVersion() ?: -1 } catch (_: Exception) { -1 }

    fun getUid(): Int = try { service?.getUid() ?: -1 } catch (_: Exception) { -1 }

    fun getPid(): Int = try { service?.getPid() ?: -1 } catch (_: Exception) { -1 }

    fun getAccuVersion(): String = try { service?.getAccuVersion() ?: "unknown" } catch (_: Exception) { "unknown" }

    private fun requireService(): IAccuService =
        service ?: throw AccuNotConnectedException()

    private fun isAccuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(AccuConstants.ACCU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }
}

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
