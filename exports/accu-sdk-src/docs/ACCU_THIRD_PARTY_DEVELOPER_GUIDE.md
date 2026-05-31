# ACCU Third-Party Developer Guide

Welcome! This guide explains how to integrate your Android app with
**Android Control Center Ultimate (ACCU)** to gain privileged system access
— install/uninstall apps, manage permissions, write system settings, run
shell commands, and more — all with the user's informed consent.

ACCU works similarly to Shizuku: you bind to a running service and call
APIs over a Binder IPC. The difference is that ACCU uses its own
permission dialog system with per-scope granularity.

---

## Prerequisites

| Requirement | Details |
|---|---|
| ACCU installed | The user must have ACCU (com.accu.controlcenter) installed |
| AccuSystemService enabled | User must open ACCU → System Service → toggle ON |
| minSdk 29 | Android 10+ required |
| AIDL enabled in Gradle | `buildFeatures { aidl = true }` |

---

## Step 1 — Copy the AIDL Files

Create the directory `app/src/main/aidl/com/accu/api/` in your project and copy:

```
aidl/com/accu/api/IAccuService.aidl
aidl/com/accu/api/IAccuPermissionCallback.aidl
aidl/com/accu/api/IAccuProcessCallback.aidl
```

**These files must stay in exactly this package path.** The AIDL package
declaration must match the directory. Do NOT change the package, interface names,
or transaction IDs — ACCU's compiled service stub and your generated proxy must
be binary-compatible.

---

## Step 2 — Copy the SDK Helper Files

Copy everything from `sdk/` into your project's source tree:

```
sdk/AccuClient.kt
sdk/AccuConstants.kt
sdk/AccuScopes.kt
sdk/AccuPermissionCodes.kt
sdk/AccuConnectionState.kt
sdk/AccuExceptions.kt
```

Recommended destination: `app/src/main/java/com/accu/sdk/`

These files provide a clean Kotlin API on top of the raw AIDL binder.
You can use the raw binder directly (`IAccuService.Stub.asInterface(binder)`)
if you prefer — see `templates/ServiceConnection_Template.kt`.

---

## Step 3 — Update app/build.gradle.kts

```kotlin
android {
    buildFeatures {
        aidl = true   // Required for AIDL compilation
    }
}

dependencies {
    // Required for AccuClient.requestPermission() suspend function
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

See `templates/BuildGradle_Template.kts` for a complete template.

---

## Step 4 — Update AndroidManifest.xml

Add the `<queries>` block so Android 11+ (API 30+) allows your app to
see and bind to the ACCU service:

```xml
<manifest ...>

    <!-- Required on API 30+ for package visibility -->
    <queries>
        <package android:name="com.accu.controlcenter" />
    </queries>

    ...
</manifest>
```

No special permissions are required in YOUR manifest. ACCU handles all
privileged operations inside its own process.

---

## Step 5 — Connect to AccuSystemService

### Recommended: AccuClient + ViewModel

```kotlin
class MyViewModel(app: Application) : AndroidViewModel(app) {

    private val accu = AccuClient(app.applicationContext)
    val accuState: StateFlow<AccuConnectionState> = accu.state

    init {
        accu.connect()   // Call in init or onStart
    }

    override fun onCleared() {
        accu.disconnect()  // Always disconnect in onCleared
    }
}
```

### In your Activity or Composable

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.accuState.collectAsState()

    when (state) {
        is AccuConnectionState.Connected    -> Text("Connected ✅")
        is AccuConnectionState.Connecting   -> CircularProgressIndicator()
        is AccuConnectionState.Error        -> Text("Error: ${(state as AccuConnectionState.Error).reason}")
        is AccuConnectionState.Disconnected -> Text("Reconnecting...")
        is AccuConnectionState.Idle         -> Text("Not started")
    }
}
```

---

## Step 6 — Request Permission

**This must happen before any privileged call.** Show the user a button:

```kotlin
// In your ViewModel
fun requestPermission() {
    viewModelScope.launch {
        val result = accu.requestPermission()  // Suspends until user responds
        when (result) {
            AccuConstants.PERMISSION_GRANTED -> { /* proceed */ }
            AccuConstants.PERMISSION_DENIED  -> { /* show "access denied" UI */ }
            else                              -> { /* service unavailable */ }
        }
    }
}
```

ACCU will display its permission bottom-sheet dialog to the user. They can
toggle individual scopes on/off before granting.

**If permission is already stored from a previous session, the callback fires
immediately with PERMISSION_GRANTED — no dialog appears.**

---

## Step 7 — Check Permission Status

```kotlin
val code = accu.checkPermission()

when (code) {
    AccuConstants.PERMISSION_GRANTED           -> { /* can call APIs */ }
    AccuConstants.PERMISSION_DENIED            -> { /* user denied */ }
    AccuConstants.PERMISSION_NOT_YET_REQUESTED -> { /* call requestPermission first */ }
    AccuConstants.PERMISSION_SERVICE_UNAVAILABLE -> { /* service not running */ }
}
```

---

## Step 8 — Check Which Scopes You Have

```kotlin
if (accu.hasScope(AccuScopes.SHELL)) {
    // Can call exec(), execAsync(), execAndGetOutput()
}

if (accu.hasScope(AccuScopes.PACKAGE_MANAGE)) {
    // Can call installApk(), disablePackage(), etc.
}

if (accu.hasScope(AccuScopes.PERMISSIONS)) {
    // Can call grantPermission(), revokePermission(), setAppOp()
}

if (accu.hasScope(AccuScopes.SETTINGS)) {
    // Can call writeSecureSetting(), readGlobalSetting(), etc.
}

if (accu.hasScope(AccuScopes.LOCALE)) {
    // Can call setApplicationLocale()
}
```

---

## Step 9 — Call APIs

**Always call on a background thread (Dispatchers.IO):**

```kotlin
// Shell execution
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        accu.exec("pm list packages -3")
    }
    val packages = result.stdout.lines()
    val exitOk = result.isSuccess
}

// Streaming shell
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        accu.execAsync(
            command  = "logcat -T 100",
            onStdout = { line -> /* handle output line */ },
            onStderr = { line -> /* handle error line */ },
            onExit   = { code -> /* command finished */ },
        )
    }
}

// Package management
viewModelScope.launch {
    val ok = withContext(Dispatchers.IO) {
        accu.disablePackage("com.example.bloatware")
    }
}

// Settings
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        accu.writeGlobalSetting("animator_duration_scale", "0")
        val brightness = accu.readSystemSetting("screen_brightness")
    }
}

// Locale
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        accu.setApplicationLocale("com.android.chrome", "ja-JP")
    }
}
```

---

## Step 10 — Handle Disconnection

AccuClient automatically reconnects when the service disconnects (e.g. after an
ACCU update). You don't need to handle this manually.

If you need to know when the service dies and comes back:

```kotlin
accuState.collect { state ->
    when (state) {
        is AccuConnectionState.Disconnected -> {
            // Service died — AccuClient is already trying to reconnect
            showSnackbar("ACCU disconnected. Reconnecting...")
        }
        is AccuConnectionState.Connected -> {
            // Back online — re-check permission status
        }
    }
}
```

---

## Step 11 — Disconnect Cleanly

```kotlin
// Call from onStop() or ViewModel.onCleared()
accu.disconnect()
```

Never leak the service connection. Always unbind when your component is destroyed.

---

## Handling Errors

```kotlin
try {
    val result = accu.exec("some command")
} catch (e: AccuNotConnectedException) {
    // Not connected — call connect() first
} catch (e: AccuScopeDeniedException) {
    // User disabled the SHELL scope — show explanation
} catch (e: AccuPermissionDeniedException) {
    // User denied permission — show requestPermission UI
} catch (e: AccuDeadServiceException) {
    // Binder died — AccuClient will auto-reconnect
} catch (e: AccuException) {
    // Base class — catch-all for any ACCU error
} catch (e: Exception) {
    // Unexpected error
}
```

---

## Checking ACCU Installation

```kotlin
val isInstalled = try {
    packageManager.getPackageInfo("com.accu.controlcenter", 0)
    true
} catch (_: PackageManager.NameNotFoundException) { false }

if (!isInstalled) {
    // Show "Please install ACCU" UI with a Play Store link
    // or guide to sideloading
}
```

AccuClient also does this check automatically in `connect()` — if ACCU is not
installed, `state` becomes `AccuConnectionState.Error` with a descriptive message.

---

## Complete Quick-Start Checklist

- [ ] Copy 3 AIDL files → `app/src/main/aidl/com/accu/api/`
- [ ] Copy 6 SDK files → `app/src/main/java/com/accu/sdk/`
- [ ] Add `aidl = true` to `android.buildFeatures` in build.gradle.kts
- [ ] Add `<queries><package android:name="com.accu.controlcenter"/></queries>` to AndroidManifest.xml
- [ ] Add coroutines dependency
- [ ] Create `AccuClient(context)` in your ViewModel
- [ ] Call `accu.connect()` in ViewModel init
- [ ] Call `accu.disconnect()` in ViewModel.onCleared()
- [ ] Observe `accu.state` in your UI
- [ ] Show "Grant Permission" button when state is Connected but not granted
- [ ] Call all ACCU APIs on Dispatchers.IO
- [ ] Test with ACCU's System Service toggled ON

---

## FAQ

**Q: Do I need Shizuku installed too?**
A: No. The user only needs ACCU installed. ACCU manages its own relationship
with Shizuku internally. As a third-party developer, you only talk to ACCU.

**Q: What if the user has ACCU but System Service is off?**
A: `bindService()` will fail. `AccuConnectionState.Error` will be emitted.
Show a message: "Please open ACCU and enable System Service."

**Q: Can I request only specific scopes, not all five?**
A: You cannot limit the dialog — ACCU always shows all 5 scopes and lets the
user decide which to enable. Design your app to gracefully degrade if a scope
is not granted (check `hasScope()` before each API call group).

**Q: Is this production-safe?**
A: Yes. The Binder IPC is stable. ACCU stores grants persistently. The API is
versioned (check `getVersion()`). Transaction IDs are frozen.

**Q: Can I publish to the Play Store?**
A: ACCU requires the device to have elevated privilege (Shizuku or root).
Your app can be published but must clearly disclose this requirement.
Calls that fail gracefully on non-rooted devices are fine.

**Q: How do I debug IPC issues?**
A: Use `adb logcat` filtered to `ACCU`. ACCU logs every call with the caller
package name and the scope it checked.
