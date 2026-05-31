# ACCU API Reference

Full reference for all 25 methods exposed by `IAccuService`.
All methods are synchronous and block the calling thread.
Always call them from `Dispatchers.IO` in a coroutine.

Transaction IDs are stable across versions.

---

## Identity (no permission required)

### `getVersion(): Int` — Transaction 1
Returns the ACCU IPC protocol version. Currently `1`.
Check this first if you need to ensure compatibility with a minimum API level.

### `getUid(): Int` — Transaction 2
Returns the UID of the AccuSystemService process.
- `0` = running as root
- `2000` = running via Shizuku (shell UID)

### `getPid(): Int` — Transaction 3
Returns the PID of the AccuSystemService process.

### `getAccuVersion(): String` — Transaction 4
Returns the human-readable ACCU app version string, e.g. `"2.0.0"`.

### `ping(): Boolean` — Transaction 5
Returns `true` if the service is alive and healthy. Use this for heartbeat checks.

---

## Permission System (call before any privileged method)

### `requestPermission(callback: IAccuPermissionCallback)` — Transaction 10
Ask ACCU to show its permission-grant dialog to the user.

The result is delivered **asynchronously** via the callback:
- `0` = `PERMISSION_GRANTED` — user tapped Grant
- `1` = `PERMISSION_DENIED` — user tapped Deny
- `-1` = `REQUEST_CANCELLED` — user dismissed the dialog

**You MUST call this before any privileged API.** On subsequent connections,
if the permission is already stored, the callback fires immediately with `GRANTED`
— no dialog is shown.

**Pattern:**
```kotlin
service.requestPermission(object : IAccuPermissionCallback.Stub() {
    override fun onPermissionResult(result: Int) {
        if (result == 0) { /* granted, proceed */ }
    }
})
```

### `checkPermission(): Int` — Transaction 11
Check if this calling package already has ACCU permission.

Returns:
- `0` = `PERMISSION_GRANTED`
- `1` = `PERMISSION_DENIED`
- `-1` = `NOT_YET_REQUESTED` (call `requestPermission` first)
- `-2` = `SERVICE_UNAVAILABLE`

Does NOT show any dialog. Safe to call at any time.

### `hasScope(scope: String): Boolean` — Transaction 12
Check if the caller has a specific named scope granted.

Scope names: `"SHELL"` | `"PACKAGE_MANAGE"` | `"PERMISSIONS"` | `"SETTINGS"` | `"LOCALE"` | `"ALL"`

Returns `false` if not connected, not granted, or scope was disabled by the user.

### `revokeSelf()` — Transaction 13
Revoke ACCU permission for this caller. After this, the user must grant again
via `requestPermission()`. Use for "Disconnect from ACCU" / "Sign out" flows.

---

## Shell Execution (requires scope: SHELL)

### `exec(command: String): String[]` — Transaction 20
Execute a shell command synchronously via `sh -c`.

**Returns** `String[3]`:
- `[0]` = stdout (may be empty)
- `[1]` = stderr (may be empty)
- `[2]` = exit code as string, e.g. `"0"` for success

**Blocks** until the command completes. Use `execAsync()` for long-running commands.

```kotlin
val result = service.exec("pm list packages -3")
val packages = result[0].lines()
val exitCode = result[2].toInt()
```

### `execAsync(command: String, callback: IAccuProcessCallback)` — Transaction 21
Execute a command and stream output lines back via callback.

`IAccuProcessCallback` has three methods:
- `onStdoutLine(line: String)` — called for each stdout line
- `onStderrLine(line: String)` — called for each stderr line
- `onExit(exitCode: Int)` — called when the process terminates

Callbacks are delivered on a **background thread** inside ACCU's process.
Post to your own Handler or use coroutines.

```kotlin
service.execAsync("logcat -T 50", object : IAccuProcessCallback.Stub() {
    override fun onStdoutLine(line: String) { /* handle line */ }
    override fun onStderrLine(line: String) { /* handle error */ }
    override fun onExit(exitCode: Int)      { /* process done */ }
})
```

### `execAndGetOutput(command: String): String` — Transaction 22
Convenience wrapper: execute and return combined stdout + stderr as one String.
Exit code is not returned. Use `exec()` if you need the exit code.

---

## Package Manager (requires scope: PACKAGE_MANAGE)

### `installApk(apkPath: String, installerPackage: String?): Boolean` — Transaction 30
Install an APK from an **absolute filesystem path**.
- `installerPackage` may be `null` (ACCU is used as installer)
- Returns `true` on success

```kotlin
val ok = service.installApk("/sdcard/Download/myapp.apk", null)
```

### `uninstallPackage(packageName: String): Boolean` — Transaction 31
Uninstall a package for the current user. Data is removed.
Equivalent to: `pm uninstall --user 0 <pkg>`

### `uninstallKeepData(packageName: String): Boolean` — Transaction 32
Uninstall for current user but keep data/cache.
Equivalent to: `pm uninstall -k --user 0 <pkg>`
Useful for "reset app" without losing saved data.

### `enablePackage(packageName: String): Boolean` — Transaction 33
Re-enable a previously disabled package.
Equivalent to: `pm enable --user 0 <pkg>`

### `disablePackage(packageName: String): Boolean` — Transaction 34
Disable a package. App becomes inaccessible but data is preserved.
Equivalent to: `pm disable-user --user 0 <pkg>`

### `hidePackage(packageName: String): Boolean` — Transaction 35
Hide a package. App is completely invisible — removed from launcher,
no push notifications, not visible to other apps. Data is preserved.
Equivalent to: `pm hide --user 0 <pkg>`

### `unhidePackage(packageName: String): Boolean` — Transaction 36
Restore a previously hidden package. App becomes visible again.
Equivalent to: `pm unhide --user 0 <pkg>`

### `suspendPackage(packageName: String): Boolean` — Transaction 37
Suspend a package. Icon shows as greyed-out; app cannot be opened.
The OS shows a "This app is paused" message if the user tries to launch it.
Equivalent to: `pm suspend --user 0 <pkg>`

### `unsuspendPackage(packageName: String): Boolean` — Transaction 38
Remove the suspension. App returns to normal.
Equivalent to: `pm unsuspend --user 0 <pkg>`

### `clearPackageData(packageName: String): Boolean` — Transaction 39
Clear ALL data for a package — equivalent to Settings → Apps → Clear Data.
The app is not uninstalled, but all user data, cache, and preferences are erased.
Equivalent to: `pm clear <pkg>`

### `enableComponent(packageName: String, componentName: String): Boolean` — Transaction 40
Enable a specific component (Activity, Service, BroadcastReceiver, or ContentProvider).
- `componentName` format: `"com.example.app/.MyService"` or `"com.example.app/com.example.app.MyService"`
Equivalent to: `pm enable <pkg>/<component>`

### `disableComponent(packageName: String, componentName: String): Boolean` — Transaction 41
Disable a specific component without affecting the rest of the app.
Equivalent to: `pm disable <pkg>/<component>`

---

## Runtime Permissions (requires scope: PERMISSIONS)

### `grantPermission(packageName: String, permission: String): Boolean` — Transaction 50
Grant a runtime permission to a package.
- `permission` = full permission name, e.g. `"android.permission.CAMERA"`
Equivalent to: `pm grant <pkg> <permission>`

### `revokePermission(packageName: String, permission: String): Boolean` — Transaction 51
Revoke a runtime permission from a package.
Equivalent to: `pm revoke <pkg> <permission>`

### `setAppOp(packageName: String, op: String, mode: String): Boolean` — Transaction 52
Set an App Op mode for a package.
- `op` = e.g. `"CAMERA"`, `"READ_CONTACTS"`, `"RECORD_AUDIO"`, `"SYSTEM_ALERT_WINDOW"`
- `mode` = `"allow"` | `"deny"` | `"ignore"` | `"default"`
Equivalent to: `appops set <pkg> <op> <mode>`

### `getAppOp(packageName: String, op: String): String` — Transaction 53
Get the current App Op mode for a package.
Returns one of: `"allow"` | `"deny"` | `"ignore"` | `"default"` | `"error"`
Equivalent to: `appops get <pkg> <op>`

---

## Activity Manager (requires scope: PACKAGE_MANAGE)

### `forceStop(packageName: String): Boolean` — Transaction 60
Force-stop a package. Kills all processes and background services.
Equivalent to: `am force-stop <pkg>`

---

## Locale (requires scope: LOCALE)

### `setApplicationLocale(packageName: String, locale: String): Boolean` — Transaction 61
Set the per-app locale for a package.
- `locale` = BCP 47 tag, e.g. `"en-US"`, `"ja-JP"`, `"zh-Hans"`, `"ar-SA"`
- Pass `""` (empty string) to reset to system locale
Equivalent to: `am set-app-locale --user 0 <pkg> --locale <locale>`

Minimum API: 33 (Android 13). On older devices, ACCU executes the command
but the OS may not honour it.

---

## System Settings (requires scope: SETTINGS)

All settings methods return `true` on success, `false` on failure.

### `writeSecureSetting(name: String, value: String): Boolean` — Transaction 70
Write a value to `Settings.Secure`. Requires WRITE_SECURE_SETTINGS, which ACCU holds.

### `readSecureSetting(name: String): String` — Transaction 71
Read a value from `Settings.Secure`. Returns empty string if not found.

### `writeGlobalSetting(name: String, value: String): Boolean` — Transaction 72
Write a value to `Settings.Global`.

### `readGlobalSetting(name: String): String` — Transaction 73
Read a value from `Settings.Global`.

### `writeSystemSetting(name: String, value: String): Boolean` — Transaction 74
Write a value to `Settings.System`.

### `readSystemSetting(name: String): String` — Transaction 75
Read a value from `Settings.System`.

---

## Common Settings Keys

| Category | Key | Values | Effect |
|---|---|---|---|
| Global | `animator_duration_scale` | `"0"` / `"1"` | Disable/enable animations |
| Global | `window_animation_scale` | `"0"` / `"1"` | Window animations |
| Global | `transition_animation_scale` | `"0"` / `"1"` | Transition animations |
| Global | `adb_enabled` | `"0"` / `"1"` | ADB on/off |
| Global | `mobile_data` | `"0"` / `"1"` | Mobile data |
| Secure | `bluetooth_on` | `"0"` / `"1"` | Bluetooth state |
| Secure | `location_providers_allowed` | `"gps,network"` | Location providers |
| Secure | `default_input_method` | package/class | Default IME |
| Secure | `enabled_accessibility_services` | pkg/class list | Accessibility services |
| System | `screen_brightness` | `0`–`255` | Display brightness |
| System | `screen_off_timeout` | ms | Screen timeout |
| System | `ringtone` | URI | Default ringtone |
