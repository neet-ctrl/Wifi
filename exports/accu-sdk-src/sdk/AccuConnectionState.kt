package com.accu.sdk

/**
 * ACCU SDK — Connection State
 *
 * Represents the current connection lifecycle between your app and
 * AccuSystemService. Observe this from your ViewModel to react to
 * service connects/disconnects without leaking the binder.
 */
sealed class AccuConnectionState {

    /** Your app has never tried to bind yet. Initial state. */
    object Idle : AccuConnectionState()

    /** bindService() was called; waiting for onServiceConnected(). */
    object Connecting : AccuConnectionState()

    /**
     * The binder is live and IAccuService is ready.
     *
     * @param permissionCode One of AccuConstants.PERMISSION_* values.
     *   Call checkPermission() after connecting to populate this.
     * @param serviceVersion The IPC protocol version (currently 1).
     * @param accuVersion Human-readable ACCU app version string.
     */
    data class Connected(
        val permissionCode: Int,
        val serviceVersion: Int,
        val accuVersion: String,
    ) : AccuConnectionState() {
        val isPermissionGranted: Boolean get() = permissionCode.isGranted()
    }

    /**
     * The service disconnected unexpectedly (ACCU was killed / updated).
     * AccuClient will automatically attempt to rebind.
     */
    object Disconnected : AccuConnectionState()

    /**
     * bindService() returned false, or the ACCU package is not installed.
     *
     * @param reason Human-readable explanation.
     */
    data class Error(val reason: String) : AccuConnectionState()
}
