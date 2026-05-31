package com.yourcompany.yourapp

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║         ACCU SDK — Raw ServiceConnection Template (no ViewModel)         ║
 * ║                                                                          ║
 * ║  Use this if you prefer to manage the ServiceConnection yourself         ║
 * ║  without AccuClient or a ViewModel. Not recommended — AccuClient is      ║
 * ║  safer and handles reconnection. Use this only for simple scripts or     ║
 * ║  non-Compose apps.                                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.accu.api.IAccuPermissionCallback
import com.accu.api.IAccuService
import com.accu.sdk.AccuConstants

class RawAccuConnection(private val context: Context) {

    private var service: IAccuService? = null
    var onConnected: ((IAccuService) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val svc = IAccuService.Stub.asInterface(binder)
            service = svc
            onConnected?.invoke(svc)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            onDisconnected?.invoke()
        }
    }

    fun bind() {
        val intent = Intent(AccuConstants.SERVICE_ACTION).apply {
            `package` = AccuConstants.ACCU_PACKAGE
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        try { context.unbindService(connection) } catch (_: Exception) {}
        service = null
    }

    /**
     * Request ACCU permission (raw callback style — no coroutines).
     */
    fun requestPermission(onResult: (Int) -> Unit) {
        service?.requestPermission(object : IAccuPermissionCallback.Stub() {
            override fun onPermissionResult(result: Int) { onResult(result) }
        }) ?: onResult(AccuConstants.PERMISSION_SERVICE_UNAVAILABLE)
    }

    /** Direct access to the raw binder. Null if not connected. */
    fun rawService(): IAccuService? = service
}

// ── Usage example (inside an Activity) ───────────────────────────────────────
//
// private val accu = RawAccuConnection(this)
//
// override fun onStart() {
//     super.onStart()
//     accu.onConnected = { svc ->
//         val permCode = svc.checkPermission()
//         if (permCode == AccuConstants.PERMISSION_GRANTED) {
//             val result = svc.exec("id")
//             Log.d("ACCU", result[0]) // stdout
//         } else {
//             accu.requestPermission { code ->
//                 if (code == AccuConstants.PERMISSION_GRANTED) {
//                     // Now you can call APIs
//                 }
//             }
//         }
//     }
//     accu.bind()
// }
//
// override fun onStop() {
//     super.onStop()
//     accu.unbind()
// }
