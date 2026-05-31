# ACCU SDK — Troubleshooting Guide

---

## 1. `bindService()` returns `false`

**Symptom:** `AccuConnectionState.Error` with message "bindService() returned false"

**Causes and fixes:**

| Cause | Fix |
|---|---|
| AccuSystemService is not running | User must open ACCU → System Service → toggle ON |
| ACCU is not installed | Show "Install ACCU" screen |
| Missing `<queries>` in manifest | Add `<queries><package android:name="com.accu.controlcenter"/></queries>` |
| Wrong intent action string | Must be `"com.accu.api.AccuSystemService"` (exact) |
| Wrong package name | Must be `"com.accu.controlcenter"` (exact) |

```kotlin
// Correct binding code:
val intent = Intent("com.accu.api.AccuSystemService").apply {
    `package` = "com.accu.controlcenter"
}
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

---

## 2. AIDL Compilation Fails

**Symptom:** Build error: `cannot find symbol IAccuService` or `IAccuService.Stub does not exist`

**Cause:** AIDL not enabled in Gradle, or AIDL files in wrong path.

**Fix:**
1. Add `buildFeatures { aidl = true }` to your `android {}` block
2. Verify AIDL files are at exactly: `app/src/main/aidl/com/accu/api/*.aidl`
3. Sync Gradle and rebuild

```
app/
└── src/
    └── main/
        └── aidl/              ← Must exist
            └── com/
                └── accu/
                    └── api/
                        ├── IAccuService.aidl
                        ├── IAccuPermissionCallback.aidl
                        └── IAccuProcessCallback.aidl
```

---

## 3. `TransactionTooLargeException`

**Symptom:** Crash when calling `exec()` with a command that produces very large output.

**Fix:** For commands that produce large output (> 1 MB), use `execAsync()` instead,
which streams line-by-line and never materializes the full output in one Binder transaction.

```kotlin
// BAD for large output:
val result = accu.exec("cat /proc/net/tcp6")  // may throw if output > 1MB

// GOOD:
accu.execAsync("cat /proc/net/tcp6",
    onStdout = { line -> processLine(line) },
    onStderr = { line -> },
    onExit   = { code -> },
)
```

---

## 4. `SecurityException` — Does Not Have ACCU Permission

**Symptom:** `SecurityException: [com.yourapp] does not have ACCU permission. Call requestPermission() first.`

**Cause:** Your app called a privileged API without requesting permission first,
or the user denied permission.

**Fix:**
```kotlin
val code = accu.checkPermission()
if (code != AccuConstants.PERMISSION_GRANTED) {
    // Must call requestPermission() and wait for GRANTED
    val result = accu.requestPermission()
    if (result != AccuConstants.PERMISSION_GRANTED) return
}
// Now safe to call APIs
```

---

## 5. `SecurityException` — Missing Scope

**Symptom:** `SecurityException: [com.yourapp] lacks the 'SHELL' scope.`

**Cause:** The user unchecked the SHELL scope (or whichever scope) when granting.

**Fix:** Check the scope before calling:
```kotlin
if (!accu.hasScope(AccuScopes.SHELL)) {
    // Show UI: "Please re-grant ACCU access with Shell scope enabled"
    // Then: accu.revokeSelf() + accu.requestPermission()
    return
}
val result = accu.exec("id")
```

To force a fresh permission grant with all scopes:
```kotlin
accu.revokeSelf()
val result = accu.requestPermission()  // dialog will show again
```

---

## 6. Callbacks Not Arriving on Main Thread

**Symptom:** `CalledFromWrongThreadException` or `android.view.ViewRootImpl$CalledFromWrongThreadException`

**Cause:** `execAsync()` callbacks are delivered on a background thread inside ACCU's process.

**Fix:** Post to the main thread explicitly:
```kotlin
// Using coroutines (recommended):
accu.execAsync(command,
    onStdout = { line ->
        viewModelScope.launch(Dispatchers.Main) {
            outputLines.add(line)
        }
    },
    ...
)

// Using Handler:
val mainHandler = Handler(Looper.getMainLooper())
accu.execAsync(command,
    onStdout = { line -> mainHandler.post { textView.append("$line\n") } },
    ...
)
```

---

## 7. `DeadObjectException` / Service Suddenly Disconnects

**Symptom:** `android.os.DeadObjectException` when calling any API.

**Cause:** ACCU was killed (update, memory pressure, user force-stop).

**Fix:** AccuClient automatically catches `DeadObjectException`, sets state to
`Disconnected`, and calls `connect()` again. Your UI should observe `accuState`
and show a "Reconnecting..." indicator. Do not retry the failed call manually —
wait for `Connected` state to return.

```kotlin
accuState.collect { state ->
    if (state is AccuConnectionState.Disconnected) {
        showToast("ACCU disconnected — reconnecting...")
    }
}
```

---

## 8. Permission Dialog Doesn't Appear

**Symptom:** `requestPermission()` hangs forever or callback never fires.

**Causes:**
- AccuSystemService is not running (check state first)
- The device is in Do Not Disturb / focus mode blocking the activity
- The activity was already started but finished before callback

**Debug steps:**
1. Verify `accuState.value is AccuConnectionState.Connected` before calling `requestPermission()`
2. Check Logcat for `ACCU:` tag — ACCU logs permission request receipt
3. Verify ACCU is not in a crash loop (check ACCU's own notification)

---

## 9. API Returns `false` Unexpectedly

**Symptom:** `disablePackage()`, `grantPermission()`, etc. return `false`.

**Debug steps:**
1. Use `exec()` to run the equivalent shell command manually and check stderr:
   ```kotlin
   val result = accu.exec("pm disable-user --user 0 com.example.app")
   Log.d("ACCU", "stdout: ${result.stdout}")
   Log.d("ACCU", "stderr: ${result.stderr}")
   Log.d("ACCU", "exit: ${result.exitCode}")
   ```
2. Common causes:
   - Package doesn't exist (`pm: Unknown package`)
   - System protected package (`Exception occurred while executing`)
   - Device admin active (must remove device admin first)
   - App is system app that cannot be uninstalled for current user

---

## 10. `AccuNotInstalledException` at Runtime

**Symptom:** AccuClient.connect() emits Error state saying ACCU is not installed.

**Fix:** Add a check before creating AccuClient:
```kotlin
fun isAccuInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo("com.accu.controlcenter", 0)
    true
} catch (_: PackageManager.NameNotFoundException) { false }

if (!isAccuInstalled(this)) {
    showInstallPrompt()  // Direct user to download ACCU
    return
}
```

---

## Logcat Debugging

Filter Logcat to see all ACCU-related logs:
```
adb logcat -s "ACCU" -s "AccuClient" -s "AccuSystemService"
```

ACCU logs:
- Every incoming bind request
- Every `requestPermission()` with caller package
- Every privileged API call with caller package and scope check result
- Grant/deny decisions

Your SDK logs are tagged `AccuClient` (add your own with a custom tag).

---

## Minimum Versions

| Component | Minimum |
|---|---|
| Android OS | API 29 (Android 10) |
| ACCU app | Any version (check `getVersion() >= 1`) |
| Kotlin | 1.9+ |
| Coroutines | 1.7+ |
| compileSdk | 34+ |
