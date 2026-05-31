# ACCU SDK

**Android Control Center Ultimate — Third-Party Integration SDK**

This package contains everything you need to integrate any Android app
with **ACCU System Service**, gaining privileged system access
(shell execution, package management, runtime permissions, system settings)
with the user's informed consent.

---

## What is ACCU?

Android Control Center Ultimate (ACCU) is an Android app that acts as a
privilege broker, similar to Shizuku. ACCU exposes a Binder IPC interface
that third-party apps can bind to and call privileged APIs on.

ACCU uses a **scope-based permission model** — users can grant or restrict
individual categories of access (Shell, Package Management, Permissions,
Settings, Locale) on a per-app basis. A Material 3 bottom-sheet dialog
guides them through the grant process.

---

## Package Contents

```
accu-sdk/
│
├── README.md                          ← You are here
│
├── docs/
│   ├── ACCU_THIRD_PARTY_DEVELOPER_GUIDE.md   ← Start here
│   ├── ACCU_API_REFERENCE.md                 ← All 25 API methods
│   ├── ACCU_ARCHITECTURE.md                  ← IPC flow diagrams
│   ├── ACCU_TROUBLESHOOTING.md               ← Common issues + fixes
│   ├── ACCU_MIGRATION_FROM_SHIZUKU.md        ← Coming from Shizuku?
│   └── ACCU_INTEGRATION_CHECKLIST.md         ← Verification checklist
│
├── aidl/
│   └── com/accu/api/
│       ├── IAccuService.aidl                 ← Primary IPC contract (25 methods)
│       ├── IAccuPermissionCallback.aidl      ← One-shot permission result callback
│       └── IAccuProcessCallback.aidl         ← Streaming shell output callback
│
├── sdk/
│   ├── AccuClient.kt                         ← Main entry point — use this
│   ├── AccuConstants.kt                      ← Service address, permission codes
│   ├── AccuScopes.kt                         ← Scope name constants + descriptions
│   ├── AccuPermissionCodes.kt                ← Extension fns for permission codes
│   ├── AccuConnectionState.kt                ← Sealed class for connection lifecycle
│   └── AccuExceptions.kt                     ← Typed exception hierarchy
│
├── templates/
│   ├── AndroidManifest_Template.xml          ← Manifest changes needed
│   ├── BuildGradle_Template.kts              ← Gradle changes needed
│   ├── MainActivity_Template.kt              ← Basic Compose UI template
│   ├── ViewModel_Template.kt                 ← Recommended ViewModel pattern
│   └── ServiceConnection_Template.kt         ← Raw binding (no ViewModel)
│
└── samples/
    ├── MinimalSample/                        ← Bare minimum — log output only
    ├── ShellSample/                          ← Terminal UI with exec + execAsync
    ├── PackageManagerSample/                 ← Disable/enable/hide/grant perms
    ├── SettingsSample/                       ← Read/write system settings + locale
    └── FullDemoApp/                          ← Complete demo covering all APIs
```

---

## 5-Minute Quick Start

### Step 1 — Copy AIDL files

Create `app/src/main/aidl/com/accu/api/` and copy all three `.aidl` files there.

### Step 2 — Copy SDK files

Copy the six `.kt` files from `sdk/` into `app/src/main/java/com/accu/sdk/`.

### Step 3 — Update Gradle

```kotlin
// app/build.gradle.kts
android {
    buildFeatures { aidl = true }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

### Step 4 — Update AndroidManifest.xml

```xml
<queries>
    <package android:name="com.accu.controlcenter" />
</queries>
```

### Step 5 — Connect and use

```kotlin
// In your ViewModel
private val accu = AccuClient(applicationContext)
val state = accu.state

init { accu.connect() }
override fun onCleared() { accu.disconnect() }

fun requestPermission() {
    viewModelScope.launch {
        val result = accu.requestPermission()
        if (result == AccuConstants.PERMISSION_GRANTED) {
            val id = withContext(Dispatchers.IO) { accu.exec("id") }
            // id.stdout = "uid=0(root) gid=0(root)..."
        }
    }
}
```

---

## Supported APIs (25 total)

| Category | Methods |
|---|---|
| Identity | `ping`, `getVersion`, `getUid`, `getPid`, `getAccuVersion` |
| Permission | `requestPermission`, `checkPermission`, `hasScope`, `revokeSelf` |
| Shell | `exec`, `execAsync`, `execAndGetOutput` |
| Package Manager | `installApk`, `uninstallPackage`, `uninstallKeepData`, `enablePackage`, `disablePackage`, `hidePackage`, `unhidePackage`, `suspendPackage`, `unsuspendPackage`, `clearPackageData`, `enableComponent`, `disableComponent`, `forceStop` |
| Permissions | `grantPermission`, `revokePermission`, `setAppOp`, `getAppOp` |
| Locale | `setApplicationLocale` |
| Settings | `writeSecureSetting`, `readSecureSetting`, `writeGlobalSetting`, `readGlobalSetting`, `writeSystemSetting`, `readSystemSetting` |

Full documentation in `docs/ACCU_API_REFERENCE.md`.

---

## Requirements

- ACCU (`com.accu.controlcenter`) installed on device
- AccuSystemService enabled in ACCU
- Android 10+ (API 29+)
- `aidl = true` in your Gradle build features

---

## For detailed integration instructions: `docs/ACCU_THIRD_PARTY_DEVELOPER_GUIDE.md`
