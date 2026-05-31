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
    boolean installApk(String apkPath, String installerPackage) = 30;
    boolean uninstallPackage(String packageName) = 31;
    boolean uninstallKeepData(String packageName) = 32;
    boolean enablePackage(String packageName) = 33;
    boolean disablePackage(String packageName) = 34;
    boolean hidePackage(String packageName) = 35;
    boolean unhidePackage(String packageName) = 36;
    boolean suspendPackage(String packageName) = 37;
    boolean unsuspendPackage(String packageName) = 38;
    boolean clearPackageData(String packageName) = 39;
    boolean enableComponent(String packageName, String componentName) = 40;
    boolean disableComponent(String packageName, String componentName) = 41;

    // ── Runtime Permissions ────────────────────────────────────────────────
    boolean grantPermission(String packageName, String permission) = 50;
    boolean revokePermission(String packageName, String permission) = 51;
    boolean setAppOp(String packageName, String op, String mode) = 52;
    String getAppOp(String packageName, String op) = 53;

    // ── Activity Manager ──────────────────────────────────────────────────
    boolean forceStop(String packageName) = 60;
    boolean setApplicationLocale(String packageName, String locale) = 61;

    // ── System Settings ────────────────────────────────────────────────────
    boolean writeSecureSetting(String name, String value) = 70;
    String readSecureSetting(String name) = 71;
    boolean writeGlobalSetting(String name, String value) = 72;
    String readGlobalSetting(String name) = 73;
    boolean writeSystemSetting(String name, String value) = 74;
    String readSystemSetting(String name) = 75;
}
