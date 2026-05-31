// IAccuPermissionCallback.aidl
package com.accu.api;

/**
 * One-shot callback delivered to the client after the user responds to
 * ACCU's permission-grant dialog.
 *
 * result values:
 *   0  = PERMISSION_GRANTED
 *   1  = PERMISSION_DENIED
 *  -1  = REQUEST_CANCELLED (dialog dismissed without a choice)
 */
oneway interface IAccuPermissionCallback {
    void onPermissionResult(int result);
}
