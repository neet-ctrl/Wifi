package com.accu.sdk

/**
 * ACCU SDK — Constants
 *
 * All stable string and integer constants used when binding to
 * AccuSystemService and calling its APIs.
 *
 * These values are guaranteed stable across ACCU versions.
 * Never hard-code them yourself — import this file.
 */
object AccuConstants {

    // ── Service binding ──────────────────────────────────────────────────────

    /** Package name of the ACCU app that hosts the service. */
    const val ACCU_PACKAGE = "com.accu.controlcenter"

    /** Intent action used to bind to AccuSystemService. */
    const val SERVICE_ACTION = "com.accu.api.AccuSystemService"

    /** IPC protocol version. Check getVersion() == PROTOCOL_VERSION before use. */
    const val PROTOCOL_VERSION = 1

    // ── Permission result codes ──────────────────────────────────────────────
    // Returned by IAccuService.checkPermission() and delivered via
    // IAccuPermissionCallback.onPermissionResult(int result).

    /** User granted ACCU permission to this caller. */
    const val PERMISSION_GRANTED = 0

    /** User explicitly denied ACCU permission to this caller. */
    const val PERMISSION_DENIED = 1

    /** Caller has never called requestPermission() yet. */
    const val PERMISSION_NOT_YET_REQUESTED = -1

    /** AccuSystemService is unavailable or not running. */
    const val PERMISSION_SERVICE_UNAVAILABLE = -2

    /** User dismissed the dialog without making a choice. */
    const val PERMISSION_REQUEST_CANCELLED = -1
}
