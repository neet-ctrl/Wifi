package com.accu.services

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import timber.log.Timber

/**
 * Legacy stub — originally ShizukuUserService, retained for binary compatibility.
 * ACCU no longer uses a separate privileged process — all privileged
 * execution is routed through AccuConnectionManager (root or wireless ADB).
 * This stub safely no-ops all bind/unbind calls.
 */
class ShizukuUserService : IShizukuUserService.Stub() {

    companion object {
        private const val TAG = "AccuUserService"

        private val userServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                Timber.d("$TAG: connected (no-op stub)")
            }
            override fun onServiceDisconnected(name: ComponentName) {
                Timber.d("$TAG: disconnected (no-op stub)")
            }
        }

        /** No-op — ACCU uses AccuConnectionManager directly; this stub is kept for compatibility. */
        fun bind() { Timber.d("$TAG: bind() called — no-op, ACCU uses AccuConnectionManager") }

        /** No-op. */
        fun unbind() { Timber.d("$TAG: unbind() called — no-op") }

        fun getService(): IShizukuUserService? = null
    }

    override fun destroy() { Timber.d("$TAG: destroy()") }
    override fun exit() { Timber.d("$TAG: exit()") }
}
