package com.accu.sdk

/**
 * ACCU SDK — Exception Hierarchy
 *
 * All exceptions thrown by AccuClient or the binder stub.
 * Catch the base class AccuException to handle any ACCU error.
 */
open class AccuException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * Thrown when AccuClient calls a privileged method but the service
 * is not yet connected. Call ensureConnected() first.
 */
class AccuNotConnectedException :
    AccuException("AccuSystemService is not connected. Did you call AccuClient.connect()?")

/**
 * Thrown when the caller does not have ACCU permission granted.
 * Call AccuClient.requestPermission() and wait for PERMISSION_GRANTED.
 */
class AccuPermissionDeniedException(packageName: String) :
    AccuException("Package '$packageName' does not have ACCU permission.")

/**
 * Thrown when the caller has permission but lacks the specific scope
 * required by the API they called.
 *
 * @param requiredScope The scope that was missing (e.g. "SHELL").
 */
class AccuScopeDeniedException(requiredScope: String) :
    AccuException("Missing ACCU scope: '$requiredScope'. Request it in the permission dialog.")

/**
 * Thrown when AccuSystemService is installed but not running.
 * Instruct the user to open ACCU and enable the System Service.
 */
class AccuServiceNotRunningException :
    AccuException("AccuSystemService is not running. Ask the user to enable it in ACCU.")

/**
 * Thrown when the ACCU app is not installed on the device.
 */
class AccuNotInstalledException :
    AccuException("ACCU (com.accu.controlcenter) is not installed on this device.")

/**
 * Wraps a DeadObjectException from the binder.
 * AccuClient catches this and emits Disconnected state automatically.
 */
class AccuDeadServiceException(cause: Throwable) :
    AccuException("ACCU service binder died unexpectedly.", cause)
