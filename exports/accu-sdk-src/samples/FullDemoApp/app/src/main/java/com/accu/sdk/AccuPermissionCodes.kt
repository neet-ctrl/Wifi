package com.accu.sdk

fun Int.isGranted()            = this == AccuConstants.PERMISSION_GRANTED
fun Int.isDenied()             = this == AccuConstants.PERMISSION_DENIED
fun Int.isNotYetRequested()    = this == AccuConstants.PERMISSION_NOT_YET_REQUESTED
fun Int.isServiceUnavailable() = this == AccuConstants.PERMISSION_SERVICE_UNAVAILABLE

fun Int.toPermissionLabel(): String = when (this) {
    AccuConstants.PERMISSION_GRANTED             -> "PERMISSION_GRANTED"
    AccuConstants.PERMISSION_DENIED              -> "PERMISSION_DENIED"
    AccuConstants.PERMISSION_NOT_YET_REQUESTED   -> "NOT_YET_REQUESTED"
    AccuConstants.PERMISSION_SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
    else                                          -> "UNKNOWN($this)"
}
