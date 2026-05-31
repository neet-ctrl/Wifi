// IAccuService.aidl
package com.accu.api;

import com.accu.api.IAccuPermissionCallback;
import com.accu.api.IAccuProcessCallback;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              ACCU SYSTEM SERVICE — PUBLIC BINDER INTERFACE               ║
 * ║                       com.accu.api.IAccuService                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * This is the primary IPC contract between ACCU and third-party client apps.
 * Obtain an instance by binding to AccuSystemService with the action
 *   "com.accu.api.AccuSystemService"
 * inside package "com.accu.controlcenter".
 *
 * ALL methods block the calling thread (synchronous).
 * Use execAsync() for streaming output without blocking.
 *
 * Permission codes returned by checkPermission() / onPermissionResult():
 *   0  PERMISSION_GRANTED
 *   1  PERMISSION_DENIED
 *  -1  NOT_YET_REQUESTED  (call requestPermission first)
 *  -2  ACCU_SERVICE_UNAVAILABLE
 *
 * Transaction IDs are stable across versions — never reuse a number.
 */
interface IAccuService {

    // ── Identity ──────────────────────────────────────────────────────────
    /** ACCU IPC protocol version (not the app version).  Currently 1. */
    int getVersion() = 1;

    /** UID of the ACCU process (usually 0 = root, or Shizuku shell uid 2000). */
    int getUid() = 2;

    /** PID of the AccuSystemService process. */
    int getPid() = 3;

    /** Human-readable ACCU app version string, e.g. "2.0.0". */
    String getAccuVersion() = 4;

    /** Returns true if the service is alive and healthy. */
    boolean ping() = 5;

    // ── Permission System ─────────────────────────────────────────────────
    /**
     * Request ACCU to show a permission-grant dialog to the user.
     * The result is delivered asynchronously via callback.
     * You MUST call this and receive GRANTED before any privileged method.
     */
    void requestPermission(IAccuPermissionCallback callback) = 10;

    /**
     * Check if this calling package already has ACCU permission.
     * Returns one of the codes listed above.
     */
    int checkPermission() = 11;

    /**
     * Check if the caller has a specific named scope granted.
     * Scope names: "SHELL" | "PACKAGE_MANAGE" | "PERMISSIONS" |
     *              "SETTINGS" | "LOCALE" | "ALL"
     */
    boolean hasScope(String scope) = 12;

    /**
     * Revoke ACCU permission for this caller.
     * After this, the user must grant again via requestPermission().
     */
    void revokeSelf() = 13;

    // ── Shell Execution ───────────────────────────────────────────────────
    /**
     * Execute a shell command synchronously via sh -c.
     * Returns a String[3]: [stdout, stderr, exitCode_as_string].
     * Requires scope: SHELL
     */
    String[] exec(String command) = 20;

    /**
     * Execute a command and stream output lines back via callback.
     * onExit() is called when the process terminates.
     * Requires scope: SHELL
     */
    void execAsync(String command, IAccuProcessCallback callback) = 21;

    /**
     * Execute a command and return combined stdout+stderr as one string.
     * Convenience wrapper around exec().
     * Requires scope: SHELL
     */
    String execAndGetOutput(String command) = 22;

    // ── Package Manager ───────────────────────────────────────────────────
    /**
     * Install an APK from an absolute filesystem path.
     * installerPackage may be null (uses ACCU as installer).
     * Requires scope: PACKAGE_MANAGE
     */
    boolean installApk(String apkPath, String installerPackage) = 30;

    /**
     * Uninstall a package for the current user (data is removed).
     * Requires scope: PACKAGE_MANAGE
     */
    boolean uninstallPackage(String packageName) = 31;

    /**
     * Uninstall for current user but keep data/cache (pm uninstall --user 0 -k).
     * Requires scope: PACKAGE_MANAGE
     */
    boolean uninstallKeepData(String packageName) = 32;

    /**
     * Enable a previously disabled component or package.
     * Requires scope: PACKAGE_MANAGE
     */
    boolean enablePackage(String packageName) = 33;

    /**
     * Disable a package (pm disable-user --user 0).
     * App becomes inaccessible but data is preserved.
     * Requires scope: PACKAGE_MANAGE
     */
    boolean disablePackage(String packageName) = 34;

    /**
     * Hide a package (pm hide --user 0).
     * App is completely invisible (soft-uninstall without data loss).
     * Requires scope: PACKAGE_MANAGE
     */
    boolean hidePackage(String packageName) = 35;

    /**
     * Unhide a previously hidden package.
     * Requires scope: PACKAGE_MANAGE
     */
    boolean unhidePackage(String packageName) = 36;

    /**
     * Suspend a package (pm suspend --user 0).
     * Icon shows as greyed-out; app cannot be opened.
     * Requires scope: PACKAGE_MANAGE
     */
    boolean suspendPackage(String packageName) = 37;

    /**
     * Unsuspend a package.
     * Requires scope: PACKAGE_MANAGE
     */
    boolean unsuspendPackage(String packageName) = 38;

    /**
     * Clear all data for a package (pm clear).
     * Equivalent to Settings → Apps → [app] → Clear Data.
     * Requires scope: PACKAGE_MANAGE
     */
    boolean clearPackageData(String packageName) = 39;

    /**
     * Enable a specific component (activity/service/receiver/provider).
     * componentName = "com.example.app/.MyService"
     * Requires scope: PACKAGE_MANAGE
     */
    boolean enableComponent(String packageName, String componentName) = 40;

    /**
     * Disable a specific component.
     * Requires scope: PACKAGE_MANAGE
     */
    boolean disableComponent(String packageName, String componentName) = 41;

    // ── Runtime Permissions ────────────────────────────────────────────────
    /**
     * Grant a runtime permission to a package.
     * permission = full name, e.g. "android.permission.CAMERA"
     * Requires scope: PERMISSIONS
     */
    boolean grantPermission(String packageName, String permission) = 50;

    /**
     * Revoke a runtime permission from a package.
     * Requires scope: PERMISSIONS
     */
    boolean revokePermission(String packageName, String permission) = 51;

    /**
     * Set an App Op mode for a package.
     * op  = e.g. "CAMERA", "READ_CONTACTS", "RECORD_AUDIO"
     * mode = "allow" | "deny" | "ignore" | "default"
     * Requires scope: PERMISSIONS
     */
    boolean setAppOp(String packageName, String op, String mode) = 52;

    /**
     * Get the current App Op mode for a package.
     * Returns one of: "allow" | "deny" | "ignore" | "default" | "error"
     * Requires scope: PERMISSIONS
     */
    String getAppOp(String packageName, String op) = 53;

    // ── Activity Manager ──────────────────────────────────────────────────
    /**
     * Force-stop a package (am force-stop).
     * Requires scope: PACKAGE_MANAGE
     */
    boolean forceStop(String packageName) = 60;

    /**
     * Set the per-app locale for a package.
     * locale = BCP 47 tag, e.g. "en-US", "ja-JP", "" (reset to system)
     * Requires scope: LOCALE
     */
    boolean setApplicationLocale(String packageName, String locale) = 61;

    // ── System Settings ────────────────────────────────────────────────────
    /**
     * Write a value to Settings.Secure.
     * Requires scope: SETTINGS
     */
    boolean writeSecureSetting(String name, String value) = 70;

    /**
     * Read a value from Settings.Secure.
     * Requires scope: SETTINGS
     */
    String readSecureSetting(String name) = 71;

    /**
     * Write a value to Settings.Global.
     * Requires scope: SETTINGS
     */
    boolean writeGlobalSetting(String name, String value) = 72;

    /**
     * Read a value from Settings.Global.
     * Requires scope: SETTINGS
     */
    String readGlobalSetting(String name) = 73;

    /**
     * Write a value to Settings.System.
     * Requires scope: SETTINGS
     */
    boolean writeSystemSetting(String name, String value) = 74;

    /**
     * Read a value from Settings.System.
     * Requires scope: SETTINGS
     */
    String readSystemSetting(String name) = 75;
}
