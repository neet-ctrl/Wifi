# ACCU System Service — Third-Party Developer Integration Guide

> **Version:** 1.0 · **Protocol version:** 1 · **Min ACCU version:** 2.0.0
>
> This guide is self-contained. You do not need any external documentation to integrate with the ACCU System Service.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Prerequisites & Compatibility](#3-prerequisites--compatibility)
4. [Adding the AIDL Files](#4-adding-the-aidl-files)
5. [Gradle & Manifest Setup](#5-gradle--manifest-setup)
6. [Connecting to AccuSystemService](#6-connecting-to-accusystemservice)
7. [The Permission System](#7-the-permission-system)
8. [Scope Reference](#8-scope-reference)
9. [Shell Execution API](#9-shell-execution-api)
10. [Package Manager API](#10-package-manager-api)
11. [Runtime Permissions API](#11-runtime-permissions-api)
12. [Activity Manager API](#12-activity-manager-api)
13. [System Settings API](#13-system-settings-api)
14. [Identity & Health API](#14-identity--health-api)
15. [Streaming Output with execAsync](#15-streaming-output-with-execasync)
16. [Error Handling & Best Practices](#16-error-handling--best-practices)
17. [Threading Model](#17-threading-model)
18. [Lifecycle Management](#18-lifecycle-management)
19. [Security Model](#19-security-model)
20. [Testing Your Integration](#20-testing-your-integration)
21. [Troubleshooting](#21-troubleshooting)
22. [Migrating from Shizuku](#22-migrating-from-shizuku)

---

## 1. Overview

**ACCU System Service** is a privileged IPC broker built into **Android Control Center Ultimate (ACCU)**. It exposes a stable AIDL interface (`IAccuService`) that lets your app call shell commands, manage packages, modify runtime permissions, control system settings, and more — all without requiring your app to hold root or Shizuku permission itself.

ACCU handles the privilege acquisition (Shizuku or root), and your app simply binds to ACCU's service and calls methods through the binder. Permission grants are per-app and user-controlled; the user sees a clear dialog the first time your app requests access.

Think of it as **Shizuku with a friendly permission UI and a richer built-in API**.

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Your Application                          │
│                                                                 │
│   AccuClient (your ServiceConnection)                           │
│       │                                                         │
│       │  bindService(Intent("com.accu.api.AccuSystemService")   │
│       │              .setPackage("com.accu.controlcenter"))     │
└───────┼─────────────────────────────────────────────────────────┘
        │ Binder (IAccuService.Stub)
┌───────▼─────────────────────────────────────────────────────────┐
│                   AccuSystemService (ACCU)                      │
│                                                                 │
│   AccuPermissionManager  ←→  SharedPreferences grant store      │
│   AccuServiceImpl        ←→  IAccuService implementation        │
│       │                                                         │
│       │ Shizuku IPC / root shell                                │
└───────┼─────────────────────────────────────────────────────────┘
        │
┌───────▼─────────────────────────────────────────────────────────┐
│                    Android System                               │
│  PackageManager · ActivityManager · Settings · Shell           │
└─────────────────────────────────────────────────────────────────┘
```

**Key points:**
- All IPC is standard Android binder — no custom protocol.
- ACCU's service runs in ACCU's own process (UID = ACCU's UID, not root or shell).
- Privileged operations are forwarded to Shizuku (UID 2000, ADB shell) or root as available.
- Your app **never** needs Shizuku bound itself — ACCU abstracts it away.

---

## 3. Prerequisites & Compatibility

| Requirement | Details |
|---|---|
| Android SDK | minSdk **29** (Android 10) |
| ACCU installed | Package: `com.accu.controlcenter` |
| ACCU version | 2.0.0+ (protocol version 1) |
| ACCU running | AccuSystemService must be started (user can enable auto-start at boot) |

**Your app does NOT need:**
- The Shizuku SDK or the Shizuku app
- Root access
- Any special system permissions

---

## 4. Adding the AIDL Files

AIDL files define the IPC contract. Copy these three files into your project, preserving the package path exactly.

### Directory layout

```
app/src/main/aidl/
└── com/
    └── accu/
        └── api/
            ├── IAccuService.aidl
            ├── IAccuPermissionCallback.aidl
            └── IAccuProcessCallback.aidl
```

### `IAccuPermissionCallback.aidl`

```aidl
// IAccuPermissionCallback.aidl
package com.accu.api;

oneway interface IAccuPermissionCallback {
    void onPermissionResult(int result);
}
```

### `IAccuProcessCallback.aidl`

```aidl
// IAccuProcessCallback.aidl
package com.accu.api;

oneway interface IAccuProcessCallback {
    void onStdoutLine(String line);
    void onStderrLine(String line);
    void onExit(int exitCode);
}
```

### `IAccuService.aidl`

```aidl
// IAccuService.aidl
package com.accu.api;

import com.accu.api.IAccuPermissionCallback;
import com.accu.api.IAccuProcessCallback;

interface IAccuService {

    // Identity
    int getVersion() = 1;
    int getUid() = 2;
    int getPid() = 3;
    String getAccuVersion() = 4;
    boolean ping() = 5;

    // Permission system
    void requestPermission(IAccuPermissionCallback callback) = 10;
    int checkPermission() = 11;
    boolean hasScope(String scope) = 12;
    void revokeSelf() = 13;

    // Shell
    String[] exec(String command) = 20;
    void execAsync(String command, IAccuProcessCallback callback) = 21;
    String execAndGetOutput(String command) = 22;

    // Package manager
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

    // Runtime permissions
    boolean grantPermission(String packageName, String permission) = 50;
    boolean revokePermission(String packageName, String permission) = 51;
    boolean setAppOp(String packageName, String op, String mode) = 52;
    String getAppOp(String packageName, String op) = 53;

    // Activity manager
    boolean forceStop(String packageName) = 60;
    boolean setApplicationLocale(String packageName, String locale) = 61;

    // System settings
    boolean writeSecureSetting(String name, String value) = 70;
    String readSecureSetting(String name) = 71;
    boolean writeGlobalSetting(String name, String value) = 72;
    String readGlobalSetting(String name) = 73;
    boolean writeSystemSetting(String name, String value) = 74;
    String readSystemSetting(String name) = 75;
}
```

> **Important:** Transaction IDs (the `= N` numbers) are stable. Never change them. Adding new methods must use new numbers that haven't been used before.

---

## 5. Gradle & Manifest Setup

### `build.gradle.kts` (app module)

No special dependency on ACCU is required. Android's build system compiles the AIDL files automatically.

```kotlin
android {
    // AIDL support is on by default in AGP — no extra config needed.
    // Make sure buildFeatures does NOT disable it:
    buildFeatures {
        aidl = true  // default is true; only add this if you've explicitly disabled it
    }
}
```

### `AndroidManifest.xml`

Declare that your app queries ACCU's package (required on Android 11+ due to package visibility):

```xml
<manifest ...>

    <!-- Required: allows your app to see and bind to ACCU's service -->
    <queries>
        <package android:name="com.accu.controlcenter" />
    </queries>

    <!-- Optional: declare the ACCU service permission your app will use -->
    <uses-permission android:name="com.accu.api.permission.BIND_ACCU_SERVICE" />

    ...
</manifest>
```

---

## 6. Connecting to AccuSystemService

Here is a complete, production-ready client class you can drop into your project:

```kotlin
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.accu.api.IAccuService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccuClient(private val context: Context) {

    companion object {
        const val ACTION   = "com.accu.api.AccuSystemService"
        const val PACKAGE  = "com.accu.controlcenter"
    }

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    var service: IAccuService? = null
        private set

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IAccuService.Stub.asInterface(binder)
            _connected.value = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            _connected.value = false
            // Optionally: schedule a reconnect here
        }
    }

    /** Call from your Activity/Fragment/ViewModel onCreate. */
    fun connect() {
        val intent = Intent(ACTION).setPackage(PACKAGE)
        val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            // ACCU is not installed or AccuSystemService is not running
            handleNotAvailable()
        }
    }

    /** Call from onDestroy to avoid leaking the ServiceConnection. */
    fun disconnect() {
        try { context.unbindService(connection) } catch (_: IllegalArgumentException) {}
        service = null
        _connected.value = false
    }

    private fun handleNotAvailable() {
        // Show a message directing the user to install/open ACCU
    }
}
```

**Usage in a ViewModel:**

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val accu = AccuClient(context)

    init {
        accu.connect()
    }

    fun runMyCommand() {
        val svc = accu.service ?: return
        val result = svc.exec("pm list packages -3")
        // result[0] = stdout, result[1] = stderr, result[2] = exit code as string
    }

    override fun onCleared() {
        accu.disconnect()
    }
}
```

---

## 7. The Permission System

Before calling any privileged API, your app must obtain permission from the user. ACCU's permission model is user-controlled: the first time your app calls `requestPermission()`, ACCU shows a dialog to the user. If granted, ACCU stores the grant persistently — your app never needs to ask again (unless the user revokes it in ACCU's Service Hub).

### Permission result codes

| Code | Constant | Meaning |
|---|---|---|
| `0` | `PERMISSION_GRANTED` | Access granted — call privileged APIs freely |
| `1` | `PERMISSION_DENIED` | User denied — do not retry without user action |
| `-1` | `NOT_YET_REQUESTED` | No decision recorded — call `requestPermission()` |
| `-2` | `SERVICE_UNAVAILABLE` | Internal error — retry after reconnect |

### Recommended permission flow

```kotlin
fun ensurePermission(accu: IAccuService, callback: IAccuPermissionCallback) {
    when (accu.checkPermission()) {
        0  -> callback.onPermissionResult(0)   // Already granted — proceed
        1  -> showDeniedUi()                   // User said no — respect it
        else -> accu.requestPermission(callback) // First time or error — ask user
    }
}
```

### Implementing `IAccuPermissionCallback`

The callback is `oneway` — it is delivered on a background binder thread, not the main thread.

```kotlin
val permissionCallback = object : IAccuPermissionCallback.Stub() {
    override fun onPermissionResult(result: Int) {
        when (result) {
            0 -> {
                // Granted — now safe to call privileged APIs
                mainHandler.post { onPermissionGranted() }
            }
            1 -> mainHandler.post { showPermissionDeniedMessage() }
        }
    }
}

// Then:
accu.requestPermission(permissionCallback)
```

### Checking for a specific scope

```kotlin
val canInstall = accu.hasScope("PACKAGE_MANAGE")
val canShell   = accu.hasScope("SHELL")
val hasAll     = accu.hasScope("ALL")
```

---

## 8. Scope Reference

Each privileged API requires the caller to hold the corresponding scope. The user grants scopes when they approve your app's permission request. ACCU's grant dialog currently grants all scopes at once ("Full Access"), but the underlying scope system allows for finer control in future versions.

| Scope name | Grants access to |
|---|---|
| `SHELL` | `exec()`, `execAsync()`, `execAndGetOutput()` |
| `PACKAGE_MANAGE` | All package install/uninstall/enable/disable/hide/suspend/clear/component methods, plus `forceStop()` |
| `PERMISSIONS` | `grantPermission()`, `revokePermission()`, `setAppOp()`, `getAppOp()` |
| `SETTINGS` | All `read*Setting()` / `write*Setting()` methods |
| `LOCALE` | `setApplicationLocale()` |
| `ALL` | All scopes — equivalent to full Shizuku/root-level access |

---

## 9. Shell Execution API

### `exec(command: String): Array<String>`

Executes a shell command synchronously via `sh -c`. Blocks the calling thread until the process exits.

**Returns:** `String[3]` — `[stdout, stderr, exitCode]`

```kotlin
val result = accu.exec("pm list packages -3")
val stdout   = result[0]   // "package:com.example.app\npackage:..."
val stderr   = result[1]   // "" on success
val exitCode = result[2]   // "0"

if (result[2] == "0") {
    val packages = stdout.lines().filter { it.startsWith("package:") }
                         .map { it.removePrefix("package:") }
}
```

### `execAndGetOutput(command: String): String`

Convenience wrapper. Returns combined stdout + stderr as a single string. Good for one-liners.

```kotlin
val info = accu.execAndGetOutput("dumpsys battery")
```

### Common shell commands

```kotlin
// List all installed packages
accu.exec("pm list packages")

// Get all running processes
accu.exec("ps -A")

// Read a system property
accu.exec("getprop ro.build.version.release")

// List files in system directory
accu.exec("ls /system/app/")

// Set airplane mode (requires SETTINGS scope too)
accu.exec("cmd connectivity airplane-mode enable")

// Kill a process by PID
accu.exec("kill -9 $pid")
```

> **Security note:** Never pass untrusted user input directly to `exec()`. Always validate and sanitize inputs.

---

## 10. Package Manager API

All methods in this section require the `PACKAGE_MANAGE` scope.

### Install an APK

```kotlin
// apkPath must be an absolute path accessible to ACCU's process
// Use a FileProvider URI resolved to a real path, or copy to a shared location first
val success = accu.installApk("/sdcard/Download/myapp.apk", null)
```

### Uninstall a package

```kotlin
accu.uninstallPackage("com.example.unwanted")       // Removes app + data
accu.uninstallKeepData("com.example.unwanted")      // Removes app, keeps data
```

### Enable / disable / hide / suspend

```kotlin
accu.disablePackage("com.example.bloatware")        // Disable (Settings → Apps shows disabled)
accu.enablePackage("com.example.bloatware")         // Re-enable
accu.hidePackage("com.example.bloatware")           // Soft-uninstall (invisible, data kept)
accu.unhidePackage("com.example.bloatware")         // Restore from hidden
accu.suspendPackage("com.example.app")              // Icon greys out; app can't be opened
accu.unsuspendPackage("com.example.app")            // Restore
```

### Clear data

```kotlin
accu.clearPackageData("com.example.app")            // Equivalent to "Clear Data" in Settings
```

### Enable / disable specific components

```kotlin
// Disable a bloatware receiver
accu.disableComponent("com.example.app", "com.example.app.analytics.TrackingReceiver")

// Re-enable it
accu.enableComponent("com.example.app", "com.example.app.analytics.TrackingReceiver")
```

### Force-stop

```kotlin
accu.forceStop("com.example.app")
```

---

## 11. Runtime Permissions API

All methods in this section require the `PERMISSIONS` scope.

### Grant / revoke a runtime permission

```kotlin
// Grant camera access to an app that has declared it in its manifest
accu.grantPermission("com.example.app", "android.permission.CAMERA")

// Revoke it
accu.revokePermission("com.example.app", "android.permission.CAMERA")
```

### App Ops

App Ops are lower-level than runtime permissions and control features like background data, notifications, exact alarm, etc.

```kotlin
// Set an App Op
accu.setAppOp("com.example.app", "CAMERA", "allow")
accu.setAppOp("com.example.app", "READ_CONTACTS", "deny")
accu.setAppOp("com.example.app", "RECORD_AUDIO", "ignore")

// Valid modes: "allow" | "deny" | "ignore" | "default"

// Read current mode
val mode = accu.getAppOp("com.example.app", "CAMERA")
// Returns: "allow" | "deny" | "ignore" | "default" | "error"
```

**Common App Op names:**

| Op | Controls |
|---|---|
| `CAMERA` | Camera access |
| `RECORD_AUDIO` | Microphone |
| `READ_CONTACTS` | Contacts |
| `ACCESS_FINE_LOCATION` | Precise location |
| `POST_NOTIFICATIONS` | Notification permission (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Exact alarms |
| `RUN_IN_BACKGROUND` | Background execution |

---

## 12. Activity Manager API

### Force-stop a package

Requires the `PACKAGE_MANAGE` scope.

```kotlin
accu.forceStop("com.example.app")
```

### Set per-app locale

Requires the `LOCALE` scope. Changes take effect immediately without restarting the app.

```kotlin
// Set Japanese for a specific app
accu.setApplicationLocale("com.example.app", "ja-JP")

// Set French
accu.setApplicationLocale("com.example.app", "fr-FR")

// Reset to system default
accu.setApplicationLocale("com.example.app", "")
```

Locale tags follow BCP 47 format. Use `""` (empty string) to reset to the system locale.

---

## 13. System Settings API

All methods in this section require the `SETTINGS` scope.

ACCU exposes read/write access to all three Android settings namespaces:

| Namespace | Methods | Notes |
|---|---|---|
| `Settings.Secure` | `readSecureSetting` / `writeSecureSetting` | Per-user security settings |
| `Settings.Global` | `readGlobalSetting` / `writeGlobalSetting` | Device-wide, one value for all users |
| `Settings.System` | `readSystemSetting` / `writeSystemSetting` | Display, sound, etc. |

### Examples

```kotlin
// Read the current default input method
val ime = accu.readSecureSetting("default_input_method")

// Enable stay-awake while charging
accu.writeGlobalSetting("stay_on_while_plugged_in", "7")

// Change screen brightness
accu.writeSystemSetting("screen_brightness", "200")

// Disable auto-brightness
accu.writeSystemSetting("screen_brightness_mode", "0")

// Read current animation scale
val animScale = accu.readGlobalSetting("window_animation_scale")
```

> **Caution:** Writing incorrect values to Settings can cause system instability. Always validate what you write. Some settings are read-only even with Shizuku.

---

## 14. Identity & Health API

Use these to verify ACCU is alive and check the privilege level.

```kotlin
// Check if service is alive (lightweight, no privilege needed)
val alive = accu.ping()

// Get ACCU app version
val accuVersion = accu.getAccuVersion()   // e.g. "2.0.0"

// Get IPC protocol version (currently 1)
val protocolVersion = accu.getVersion()

// Get the UID of the ACCU process
// 0 = root, 2000 = ADB shell (Shizuku)
val uid = accu.getUid()

// Get the PID of the AccuSystemService process
val pid = accu.getPid()
```

**Checking privilege level:**

```kotlin
when (accu.getUid()) {
    0    -> log("Running with root privileges")
    2000 -> log("Running via Shizuku (ADB shell)")
    else -> log("Running with limited privileges (uid=${accu.getUid()})")
}
```

---

## 15. Streaming Output with `execAsync`

For long-running commands (logcat, ping, network scans, etc.), use `execAsync` to receive output line-by-line without blocking a thread.

```kotlin
val processCallback = object : IAccuProcessCallback.Stub() {
    override fun onStdoutLine(line: String) {
        // Called on a background binder thread for each stdout line
        mainHandler.post { appendLine(line) }
    }

    override fun onStderrLine(line: String) {
        mainHandler.post { appendError(line) }
    }

    override fun onExit(exitCode: Int) {
        mainHandler.post { onProcessFinished(exitCode) }
    }
}

// Start the process
accu.execAsync("logcat -v brief", processCallback)

// To stop: there is no cancel API in protocol v1.
// Use exec("kill $pid") or start a fixed-duration command.
```

**Good use cases for `execAsync`:**
- `logcat` streaming
- `ping` to a host
- `tcpdump` or network captures
- Long-running file operations with progress output
- `adb shell` interactive commands

---

## 16. Error Handling & Best Practices

### Always null-check the service

The binder can become null at any time (if ACCU is stopped, updated, or crashes).

```kotlin
val svc = accu.service
if (svc == null) {
    showServiceUnavailableMessage()
    return
}
```

### Catch `RemoteException`

All IAccuService methods can throw `RemoteException` if the binder dies mid-call.

```kotlin
try {
    val result = accu.exec("pm list packages")
    // handle result
} catch (e: RemoteException) {
    // Service died — reconnect or show error
    accu.connect()
}
```

### Never call blocking IPC on the main thread

`exec()` blocks until the shell command completes. Always call it on a background thread:

```kotlin
// Correct: use a coroutine
viewModelScope.launch(Dispatchers.IO) {
    val result = accu.exec("find /sdcard -name '*.apk'")
    withContext(Dispatchers.Main) { showResults(result[0]) }
}
```

### Check the exit code

```kotlin
val result = accu.exec("pm disable-user --user 0 com.example.app")
if (result[2] != "0") {
    // Command failed
    log("Error: ${result[1]}")
}
```

### Check scope before calling

```kotlin
if (!accu.hasScope("SHELL")) {
    showScopeRequiredMessage("Shell execution")
    return
}
val result = accu.exec("ps -A")
```

---

## 17. Threading Model

| Method | Calling thread | Delivery thread |
|---|---|---|
| `exec()` | **Must be background** — blocks until exit | Same thread (blocking) |
| `execAsync()` | Any | `onStdoutLine`, `onStderrLine`, `onExit` delivered on binder thread |
| `requestPermission()` | Any | `onPermissionResult` delivered on binder thread |
| All other methods | **Must be background** — binder IPC | Same thread (blocking) |

**Rule:** Treat all `IAccuService` method calls like network I/O — never call them on the main thread. Use coroutines with `Dispatchers.IO`, a background executor, or a `HandlerThread`.

---

## 18. Lifecycle Management

### Bind / unbind properly

Bind in `onCreate`/`onStart`, unbind in `onDestroy`/`onStop`. If you use a ViewModel, bind in `init {}` and unbind in `onCleared()`.

```kotlin
// Activity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    accuClient.connect()
}

override fun onDestroy() {
    super.onDestroy()
    accuClient.disconnect()  // ALWAYS unbind to avoid leaking the connection
}
```

### Handle service disconnects

`onServiceDisconnected` is called if ACCU crashes or is killed. Implement a reconnect strategy:

```kotlin
override fun onServiceDisconnected(name: ComponentName) {
    service = null
    _connected.value = false
    // Retry after 3 seconds
    handler.postDelayed({ connect() }, 3_000)
}
```

### Check if ACCU is installed

Before calling `bindService`, verify ACCU is installed:

```kotlin
fun isAccuInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.accu.controlcenter", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }
}
```

---

## 19. Security Model

### How ACCU decides whether to grant access

1. When your app first calls `requestPermission()`, ACCU shows a system-level dialog to the device owner.
2. The user chooses **Grant** or **Deny**. ACCU records the decision in its private SharedPreferences.
3. On subsequent calls, ACCU checks the stored grant — **no dialog is shown** if already decided.
4. The user can revoke your app's access at any time from **ACCU → Service Hub → Apps tab**.

### Caller identity

ACCU identifies callers by **package name** (verified via `Binder.getCallingUid()` → `PackageManager`). You cannot spoof your package name.

### The `BIND_ACCU_SERVICE` permission

ACCU's service requires the permission `com.accu.api.permission.BIND_ACCU_SERVICE` (protection level: `normal`). Declare `<uses-permission>` for it in your manifest (see Section 5). Without it, `bindService` will fail on Android 14+ with a security exception.

### Principle of least privilege

Only request the capabilities your app needs:
- A debloater app needs `PACKAGE_MANAGE` only.
- A settings tweaker needs `SETTINGS` only.
- A developer tool that runs arbitrary shell commands needs `SHELL`.

Currently ACCU grants `ALL` scopes by default; future versions will support per-scope grant selection.

---

## 20. Testing Your Integration

### Step-by-step checklist

- [ ] ACCU is installed and AccuSystemService is running (check ACCU → Service Hub — status dot is green)
- [ ] Your manifest has `<queries>` for `com.accu.controlcenter`
- [ ] Your AIDL files are in the correct package path (`com/accu/api/`)
- [ ] `bindService` returns `true` (not `false`)
- [ ] `onServiceConnected` fires and `IAccuService.Stub.asInterface(binder)` returns non-null
- [ ] `accu.ping()` returns `true`
- [ ] `accu.checkPermission()` returns `0` after the user grants access
- [ ] `accu.exec("id")` returns `["uid=2000(shell)", "", "0"]` or root equivalent

### Testing without a physical device (emulator)

1. Start an Android emulator (API 29+).
2. Install ACCU from a debug build.
3. Enable Shizuku via USB ADB: `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`
4. Open ACCU and start AccuSystemService in Service Hub.
5. Install your test app and verify the permission dialog appears.

### ADB commands for verification

```bash
# Check if AccuSystemService is running
adb shell dumpsys activity services com.accu.controlcenter | grep AccuSystemService

# Check the calling UID your app uses
adb shell dumpsys package com.your.app | grep userId

# Force-grant ACCU bind permission if testing
adb shell pm grant com.your.app com.accu.api.permission.BIND_ACCU_SERVICE
```

---

## 21. Troubleshooting

### `bindService` returns `false`

- ACCU is not installed → install ACCU first.
- AccuSystemService is not running → open ACCU → Service Hub → tap "Start".
- Your manifest is missing `<queries>` → add it (required on API 30+).
- Intent is wrong → must use action `"com.accu.api.AccuSystemService"` **and** `.setPackage("com.accu.controlcenter")`.

### `onServiceConnected` never fires

- `bindService` returned `true` but ACCU service is not actually running (it was started manually but crashed) → check ACCU's notification drawer — the persistent notification disappears when stopped.
- Your `ServiceConnection` is garbage-collected → hold a strong reference to it (keep it in a field, not a local variable).

### `RemoteException: Binder has died`

- ACCU was killed or crashed mid-call.
- Implement `onServiceDisconnected` and reconnect.

### Permission dialog never appears

- You called `requestPermission()` but the callback is never fired.
- ACCU's `AccuPermissionRequestActivity` may be blocked by the device's battery optimizer → exempt ACCU from battery optimization in device Settings.
- Check that your callback object is not garbage-collected (the binder holds a weak reference — keep a strong reference yourself).

### `exec()` returns exit code `1` or `126`

- Exit code `1` = command not found or permission denied at the shell level. Check the stderr output (`result[1]`).
- Exit code `126` = command is not executable.
- Exit code `127` = command not found in PATH.

### `installApk` returns `false`

- The APK path is not readable by ACCU's process. Copy the APK to external storage or use `exec("pm install /path/to/app.apk")` instead.
- The device is running Android 14+ and requires `PackageInstaller` sessions for non-root installs.

### Settings write returns `false`

- The setting may be read-only even with Shizuku (e.g., some `Settings.Global` keys are write-restricted).
- Use `exec("settings put global/secure/system <key> <value>")` as a fallback — it has broader write access than the Java `Settings` API under ADB shell.

---

## 22. Migrating from Shizuku

If your app already uses the Shizuku SDK and you want to support ACCU as an alternative (or replacement), here is a direct comparison and migration guide.

### Comparison table

| Feature | Shizuku | ACCU System Service |
|---|---|---|
| SDK size | ~200 KB (Shizuku API JAR) | Zero — only AIDL files |
| Privilege source | Shizuku app (ADB shell) | ACCU (which itself uses Shizuku or root) |
| Permission model | Per-app dialog in Shizuku | Per-app dialog in ACCU |
| Shell execution | `ShizukuRemoteProcess` | `accu.exec()` / `accu.execAsync()` |
| Package management | Via custom UserService | Built-in API methods |
| Settings access | Via custom UserService | Built-in `readSecureSetting()` etc. |
| User installs required | Shizuku | ACCU (which bundles its own Shizuku integration) |

### Migration mapping

| Shizuku pattern | ACCU equivalent |
|---|---|
| `Shizuku.bindUserService(...)` | `context.bindService(Intent("com.accu.api.AccuSystemService"))` |
| `Shizuku.requestPermission(code)` | `accu.requestPermission(callback)` |
| `Shizuku.checkSelfPermission()` | `accu.checkPermission()` |
| `Shizuku.newProcess(arrayOf("sh"), ...)` | `accu.exec("your command")` |
| Custom AIDL `IUserService` | Not needed — use ACCU's built-in methods |
| `Runtime.getRuntime().exec(...)` via UserService | `accu.exec("...")` |

### Side-by-side code example

**Before (Shizuku):**
```kotlin
// 1. Declare IUserService AIDL
// 2. Implement UserService
// 3. Bind with Shizuku.bindUserService
// 4. Call methods on IUserService
val args = Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, MyUserService::class.java.name))
    .daemon(false)
    .processNameSuffix("service")
    .debuggable(BuildConfig.DEBUG)
    .version(1)

Shizuku.bindUserService(args, connection)

// In onServiceConnected:
val svc = IMyUserService.Stub.asInterface(binder)
svc.runCommand("pm list packages")
```

**After (ACCU):**
```kotlin
// No UserService needed. Just bind and call exec().
val intent = Intent("com.accu.api.AccuSystemService").setPackage("com.accu.controlcenter")
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

// In onServiceConnected:
val accu = IAccuService.Stub.asInterface(binder)
accu.exec("pm list packages")
```

### Dual-mode support (Shizuku + ACCU)

If you want to support both Shizuku (for users who don't have ACCU) and ACCU (for richer API), use an abstraction:

```kotlin
interface PrivilegedExecutor {
    fun exec(command: String): String
    fun isAvailable(): Boolean
}

class AccuExecutor(private val accu: IAccuService?) : PrivilegedExecutor {
    override fun isAvailable() = accu != null && runCatching { accu.ping() }.getOrDefault(false)
    override fun exec(command: String) = accu?.execAndGetOutput(command) ?: error("ACCU unavailable")
}

class ShizukuExecutor : PrivilegedExecutor {
    override fun isAvailable() = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PERMISSION_GRANTED
    override fun exec(command: String): String {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        return process.inputStream.bufferedReader().readText()
    }
}

// In your ViewModel:
val executor: PrivilegedExecutor = when {
    accuClient.connected.value -> AccuExecutor(accuClient.service)
    ShizukuExecutor().isAvailable() -> ShizukuExecutor()
    else -> error("No privileged executor available")
}
```

---

## Appendix A — Full Method Reference

| Method | Signature | Scope | Blocking |
|---|---|---|---|
| `getVersion` | `() → Int` | None | Yes |
| `getUid` | `() → Int` | None | Yes |
| `getPid` | `() → Int` | None | Yes |
| `getAccuVersion` | `() → String` | None | Yes |
| `ping` | `() → Boolean` | None | Yes |
| `requestPermission` | `(callback) → Unit` | None | No (async) |
| `checkPermission` | `() → Int` | None | Yes |
| `hasScope` | `(scope: String) → Boolean` | None | Yes |
| `revokeSelf` | `() → Unit` | None | Yes |
| `exec` | `(command: String) → String[3]` | SHELL | Yes |
| `execAsync` | `(command, callback) → Unit` | SHELL | No (streaming) |
| `execAndGetOutput` | `(command: String) → String` | SHELL | Yes |
| `installApk` | `(path, installer) → Boolean` | PACKAGE_MANAGE | Yes |
| `uninstallPackage` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `uninstallKeepData` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `enablePackage` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `disablePackage` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `hidePackage` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `unhidePackage` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `suspendPackage` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `unsuspendPackage` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `clearPackageData` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `enableComponent` | `(pkg, component) → Boolean` | PACKAGE_MANAGE | Yes |
| `disableComponent` | `(pkg, component) → Boolean` | PACKAGE_MANAGE | Yes |
| `grantPermission` | `(pkg, permission) → Boolean` | PERMISSIONS | Yes |
| `revokePermission` | `(pkg, permission) → Boolean` | PERMISSIONS | Yes |
| `setAppOp` | `(pkg, op, mode) → Boolean` | PERMISSIONS | Yes |
| `getAppOp` | `(pkg, op) → String` | PERMISSIONS | Yes |
| `forceStop` | `(pkg) → Boolean` | PACKAGE_MANAGE | Yes |
| `setApplicationLocale` | `(pkg, locale) → Boolean` | LOCALE | Yes |
| `writeSecureSetting` | `(name, value) → Boolean` | SETTINGS | Yes |
| `readSecureSetting` | `(name) → String` | SETTINGS | Yes |
| `writeGlobalSetting` | `(name, value) → Boolean` | SETTINGS | Yes |
| `readGlobalSetting` | `(name) → String` | SETTINGS | Yes |
| `writeSystemSetting` | `(name, value) → Boolean` | SETTINGS | Yes |
| `readSystemSetting` | `(name) → String` | SETTINGS | Yes |

---

## Appendix B — Minimal Sample App

A minimal Android app that binds to ACCU and runs a shell command:

**`MainActivity.kt`**
```kotlin
class MainActivity : AppCompatActivity() {
    private var accu: IAccuService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            accu = IAccuService.Stub.asInterface(b)
            val cb = object : IAccuPermissionCallback.Stub() {
                override fun onPermissionResult(result: Int) {
                    if (result == 0) runOnUiThread { doWork() }
                }
            }
            when (accu?.checkPermission()) {
                0    -> runOnUiThread { doWork() }
                else -> accu?.requestPermission(cb)
            }
        }
        override fun onServiceDisconnected(n: ComponentName) { accu = null }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        bindService(
            Intent("com.accu.api.AccuSystemService").setPackage("com.accu.controlcenter"),
            connection, BIND_AUTO_CREATE,
        )
    }

    override fun onDestroy() { super.onDestroy(); unbindService(connection) }

    private fun doWork() {
        Thread {
            val result = accu?.exec("pm list packages -3") ?: return@Thread
            runOnUiThread { textView.text = result[0] }
        }.start()
    }
}
```

---

*ACCU System Service developer guide · © 2025 ACC Ultimate Contributors · Apache 2.0*
