package com.accu.sdk

sealed class AccuConnectionState {
    object Idle : AccuConnectionState()
    object Connecting : AccuConnectionState()
    data class Connected(
        val permissionCode: Int,
        val serviceVersion: Int,
        val accuVersion: String,
    ) : AccuConnectionState() {
        val isPermissionGranted: Boolean get() = permissionCode.isGranted()
    }
    object Disconnected : AccuConnectionState()
    data class Error(val reason: String) : AccuConnectionState()
}
