package com.accu.sdk

open class AccuException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class AccuNotConnectedException :
    AccuException("AccuSystemService is not connected. Did you call AccuClient.connect()?")

class AccuPermissionDeniedException(packageName: String) :
    AccuException("Package '$packageName' does not have ACCU permission.")

class AccuScopeDeniedException(requiredScope: String) :
    AccuException("Missing ACCU scope: '$requiredScope'. Request it in the permission dialog.")

class AccuServiceNotRunningException :
    AccuException("AccuSystemService is not running. Ask the user to enable it in ACCU.")

class AccuNotInstalledException :
    AccuException("ACCU (com.accu.controlcenter) is not installed on this device.")

class AccuDeadServiceException(cause: Throwable) :
    AccuException("ACCU service binder died unexpectedly.", cause)
