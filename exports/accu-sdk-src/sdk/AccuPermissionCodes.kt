package com.accu.sdk

/**
 * ACCU SDK — Permission Code Utilities
 *
 * Typed extension functions and helpers for working with the integer
 * permission codes returned by IAccuService.checkPermission() and
 * IAccuPermissionCallback.onPermissionResult().
 *
 * Usage:
 *   val code = accuService.checkPermission()
 *   if (code.isGranted()) { ... }
 *
 * All raw integer constants live in AccuConstants.
 */

fun Int.isGranted()            = this == AccuConstants.PERMISSION_GRANTED
fun Int.isDenied()             = this == AccuConstants.PERMISSION_DENIED
fun Int.isNotYetRequested()    = this == AccuConstants.PERMISSION_NOT_YET_REQUESTED
fun Int.isServiceUnavailable() = this == AccuConstants.PERMISSION_SERVICE_UNAVAILABLE

/**
 * Human-readable label for a permission result code.
 * Useful for logging and debug UIs.
 */
fun Int.toPermissionLabel(): String = when (this) {
    AccuConstants.PERMISSION_GRANTED          -> "GRANTED"
    AccuConstants.PERMISSION_DENIED           -> "DENIED"
    AccuConstants.PERMISSION_NOT_YET_REQUESTED -> "NOT_YET_REQUESTED"
    AccuConstants.PERMISSION_SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
    else                                       -> "UNKNOWN($this)"
}
