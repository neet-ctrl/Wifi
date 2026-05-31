# ACCU SDK — Full Demo App

> **`com.accu.sdkdemo`** — A production-quality, 14-screen Jetpack Compose reference app demonstrating every API surface of the ACCU SDK.

---

## What is this?

This is the canonical end-to-end sample app for the **ACCU SDK** (ACCU System Control & Command Utility SDK). Every feature of the IPC bridge — permissions, shell execution, package management, runtime permission ops, settings read/write, and per-app locale — is exercised with live results on a real device.

If you are building an app that integrates the ACCU SDK, clone this project and run it first. It will tell you immediately whether your device has ACCU installed, whether the service is connected, which scopes are granted, and whether each API call succeeds.

---

## Requirements

| Requirement | Minimum |
|-------------|---------|
| Android OS  | **9.0 (API 29)** |
| ACCU app    | Installed (`com.accu.controlcenter`) |
| Build tools | Android Studio Hedgehog+ · Gradle 8.7 · JDK 17 |
| Kotlin      | 2.0.21 |
| Compose BOM | 2024.10.00 |

---

## Quick Start

```bash
# 1 — Clone / open in Android Studio
git clone <repo>
cd exports/accu-sdk-src/samples/FullDemoApp

# 2 — Build debug APK
./gradlew assembleDebug

# 3 — Install to connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **Note:** `gradle-wrapper.jar` is not committed. Android Studio downloads it automatically on first sync. For CI, the GitHub Actions workflow downloads it from the Gradle GitHub mirror.

---

## App Structure

```
FullDemoApp/
├── app/src/main/
│   ├── aidl/com/accu/api/         # 3 AIDL interfaces (IAccuService, callbacks)
│   ├── java/com/accu/
│   │   ├── sdk/                   # AccuClient, AccuConnectionState, AccuConstants,
│   │   │                          #   AccuScopes, AccuPermissionCodes, AccuExceptions
│   │   └── sdkdemo/
│   │       ├── AccuSdkTestApp.kt  # Application class
│   │       ├── MainActivity.kt    # ModalNavigationDrawer + NavHost
│   │       ├── data/              # Models, LogManager, CrashManager
│   │       ├── navigation/        # NavGraph.kt (Screen sealed class, drawer sections)
│   │       ├── ui/
│   │       │   ├── components/    # StatusIndicator, StatusDot, StatusBadge, StatusRow
│   │       │   ├── screens/       # All 14 screens (see table below)
│   │       │   └── theme/         # Color.kt, Theme.kt (Material 3, dynamic color)
│   │       └── viewmodel/         # MainViewModel.kt (all state + all API calls)
│   └── res/
│       ├── drawable/              # Adaptive icon foreground vector
│       ├── mipmap-anydpi-v26/     # Adaptive icon XMLs
│       └── values/                # strings.xml, themes.xml, icon background color
├── gradle/
│   ├── libs.versions.toml         # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties  # Gradle 8.7
├── app/build.gradle.kts           # compose=true, aidl=true, coroutines dep
├── build.gradle.kts               # Root (plugins, apply false)
├── settings.gradle.kts            # Single :app module
├── gradlew / gradlew.bat          # Wrapper scripts
└── .github/workflows/
    └── build-full-demo-app.yml    # CI: builds debug APK on every push
```

---

## 14 Screens

| # | Screen | Route | What it covers |
|---|--------|-------|----------------|
| 1 | **Dashboard** | `dashboard` | Live connection state, permission badge, quick actions, log/crash counters |
| 2 | **Connection Diagnostics** | `connection` | 10-point deep diagnostic run: installed → visible → binder → ping → version → permission → scopes → version → UID → PID |
| 3 | **Permission Test** | `permission` | Permission state card, `requestPermission()` dialog, `checkPermission()`, `revokeSelf()`, code reference |
| 4 | **Scope Inspector** | `scopes` | Live `hasScope()` check for all 5 scopes with API list and transaction IDs |
| 5 | **Shell Test** | `shell` | `exec()` (sync) and `execAsync()` (streaming), 10 preset commands, copy/clear output |
| 6 | **Package Manager** | `packages` | Searchable app list; enable, disable, hide, unhide, suspend, unsuspend, clearData, forceStop per app |
| 7 | **Permission Ops** | `permops` | `grantPermission()` / `revokePermission()` for runtime permissions; `getAppOp()` / `setAppOp()` with presets |
| 8 | **Settings Test** | `settings` | Tabbed Secure / Global / System; read + safe-write for 18 preset keys |
| 9 | **Locale Test** | `locale` | Pick any installed app → apply one of 14 locale presets via `setApplicationLocale()` |
| 10 | **API Explorer** | `apiexplorer` | All 39 API methods with signature, scope, transaction ID, live test button for testable methods |
| 11 | **Log Center** | `logs` | Searchable real-time log stream; filter by level (DEBUG / INFO / SUCCESS / WARNING / ERROR); copy, clear, export JSON |
| 12 | **Crash Center** | `crashes` | Exception list with expandable stack traces, simulate button, clear all |
| 13 | **Diagnostics Export** | `export` | Full diagnostics report generator; copy as text, copy as JSON, share via Android share sheet |
| 14 | **Automated Tests** | `autotests` | 17-test validation suite with progress bar and PASS / FAIL / WARN per test; overall health summary |

---

## SDK Architecture

```
Your App                 ACCU (com.accu.controlcenter)
─────────                ──────────────────────────────
AccuClient               AccuSystemService (runs as root / Shizuku)
  │  bindService()  ──►  IAccuService (AIDL binder)
  │  ◄── onBound()
  │
  ├─ connect() / disconnect()
  ├─ checkPermission() / requestPermission(callback)  ─► IAccuPermissionCallback.aidl
  ├─ hasScope(scope)
  ├─ exec(cmd) / execAsync(cmd, callback)             ─► IAccuProcessCallback.aidl
  ├─ enablePackage() / disablePackage() / ...         (PACKAGE_MANAGE scope)
  ├─ grantPermission() / revokePermission() / ...     (PERMISSIONS scope)
  ├─ read/write Secure/Global/System Setting          (SETTINGS scope)
  └─ setApplicationLocale()                           (LOCALE scope)
```

### Connection States

```kotlin
sealed class AccuConnectionState {
    object Idle                                   : AccuConnectionState()
    object Connecting                             : AccuConnectionState()
    data class Connected(
        val serviceVersion : Int,
        val accuVersion    : String,
        val permissionCode : Int,
    )                                             : AccuConnectionState()
    object Disconnected                           : AccuConnectionState()
    data class Error(val reason: String)          : AccuConnectionState()
}
```

### Permission Codes

| Code | Constant | Meaning |
|------|----------|---------|
| `0`  | `PERMISSION_GRANTED` | All API calls available |
| `1`  | `PERMISSION_DENIED` | User explicitly denied |
| `2`  | `PERMISSION_NOT_YET_REQUESTED` | No dialog shown yet |
| `3`  | `PERMISSION_SERVICE_UNAVAILABLE` | ACCU not connected |

### Scopes

| Scope | API Group | Transaction IDs |
|-------|-----------|-----------------|
| `SHELL` | `exec`, `execAsync`, `execAndGetOutput` | 20–22 |
| `PACKAGE_MANAGE` | install/enable/disable/hide/suspend/clear/forceStop/component | 30–41, 60 |
| `PERMISSIONS` | `grantPermission`, `revokePermission`, `setAppOp`, `getAppOp` | 50–53 |
| `SETTINGS` | read/write Secure, Global, System | 70–75 |
| `LOCALE` | `setApplicationLocale` | 61 |

---

## Minimal Integration (3 steps)

**1. Declare visibility in `AndroidManifest.xml`:**
```xml
<queries>
    <package android:name="com.accu.controlcenter" />
    <intent>
        <action android:name="com.accu.api.AccuSystemService" />
    </intent>
</queries>
```

**2. Connect in your `ViewModel`:**
```kotlin
class MyViewModel(app: Application) : AndroidViewModel(app) {
    val accu = AccuClient(app)

    init {
        accu.connect()
    }

    override fun onCleared() = accu.disconnect()
}
```

**3. Request permission and call APIs:**
```kotlin
// Request (shows ACCU dialog)
viewModelScope.launch {
    val code = accu.requestPermission()
    if (code.isGranted()) {
        val result = accu.exec("id")
        println(result.stdout) // uid=0(root) ...
    }
}

// Observe connection state in Compose
val state by vm.accu.state.collectAsState()
```

---

## CI / GitHub Actions

Workflow: **`.github/workflows/build-full-demo-app.yml`**

Triggers automatically on any push that touches `exports/accu-sdk-src/samples/FullDemoApp/**` — including pushing the entire workspace from the repository root.

| Job | Description |
|-----|-------------|
| `build-debug` | Downloads Gradle wrapper JAR → builds debug APK → uploads as artifact |
| `lint` | Non-blocking lint check (runs on PRs or via `workflow_dispatch` toggle) |

Artifacts are kept for **30 days** and named `FullDemoApp-debug-build<N>`.

---

## Key Design Decisions

- **Single `MainViewModel`** — all 14 screens inject only the VM. No screen-local ViewModels. This avoids duplicate connections and keeps all state synchronized.
- **`LogManager` / `CrashManager` as object singletons** — they outlive any ViewModel, so logs survive screen rotations and back navigation. Exposed as `StateFlow` for Compose observation.
- **`AccuClient` inside the VM** — it follows the lifecycle automatically. `connect()` in `init`, `disconnect()` in `onCleared()`.
- **No binary resources committed** — `gradle-wrapper.jar` is downloaded by CI. Mipmap icons use adaptive vector XML only.
- **`ModalNavigationDrawer` navigation** — 4 drawer sections mirror the logical test groupings (Overview / Connection / API Testing / Diagnostics).

---

## License

Same license as the parent ACCU SDK project.
