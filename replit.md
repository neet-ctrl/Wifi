# Android Control Center Ultimate (ACCU)

A self-contained Android app that merges 17 open-source tools into one Material 3 powerhouse. ACCU is its own privilege broker — no Shizuku, no external ADB tools needed. Users connect once via Root, Wireless ADB, or OTG USB, and all 85+ screens share that single connection.

## Android Project Location

The Android app source is at: `android-control-center/`

```
android-control-center/
├── app/src/main/java/com/accu/
│   ├── connection/AccuConnectionManager.kt   ← global privilege singleton
│   ├── utils/ShizukuUtils.kt                 ← thin wrapper over AccuConnectionManager
│   ├── ui/shizuku/ShizukuCenterScreen.kt     ← ACCU Center UI (connection buttons)
│   ├── ui/shizuku/ShizukuViewModel.kt        ← connection management ViewModel
│   ├── ui/shizuku/AdbPairingScreen.kt        ← 4-step wireless ADB wizard
│   └── ui/shell/AdbTutorialScreen.kt         ← in-app connection guide
├── README.md                                 ← full connection guide + architecture
├── HOW_TO_TEST.md                            ← step-by-step test guide for all features
└── FEATURE_PROMISE.md                        ← complete feature coverage record
```

## Build Commands

```bash
# Debug build
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Install to connected device
./gradlew installDebug

# Release build (set signing env vars first)
./gradlew assembleRelease
```

## Stack

- Kotlin 2.1 · Jetpack Compose · Material 3 Expressive
- Architecture: MVVM + Clean, Hilt DI, Navigation Compose
- Database: Room (16 tables) + DataStore Preferences
- Privilege: LibSU (root) · Android NsdManager mDNS (wireless ADB) · USB OTG ADB
- Build: Gradle AGP 8.7, KSP, esbuild for scripts

## Privilege Architecture — AccuConnectionManager

**The single most important design decision:**

All 85+ screens, all QS tiles, all background services inject `AccuConnectionManager` via Hilt and call `exec("shell command")`. The manager auto-selects the best available method:

```
1. Root (LibSU)         → uid=0, full privilege
2. Wireless ADB         → adb -s ip:port shell <cmd>, uid=2000
3. OTG / USB ADB        → adb shell <cmd>, uid=2000
4. Plain shell fallback → sh -c <cmd>, app uid, limited
```

**Never use `Runtime.getRuntime().exec("su -c ...")` directly.** Always call `connectionManager.exec(command)`.

**New QS Tiles** must use `@AndroidEntryPoint` + `@Inject lateinit var connectionManager: AccuConnectionManager`.

## Connection Flow Summary

### Wireless ADB (Android 11+)
1. Target: `Settings → Developer Options → Wireless debugging → ON`
2. Target: `Wireless debugging → Pair device with pairing code` → note 6-digit code + pairing port
3. ACCU host: `ACCU Center → Wireless ADB button` → mDNS discovers target automatically
4. ACCU host: Enter the 6-digit code → ACCU runs `adb pair` then `adb connect`
5. Done — status card shows green, all features active

### OTG / USB ADB
1. Target: `Settings → Developer Options → USB debugging → ON`
2. Connect phones with USB OTG cable/adapter
3. ACCU host: `ACCU Center → OTG / USB button` → runs `adb devices`
4. Target: Approve "Allow USB debugging?" dialog
5. Done — all features active

### Root
1. ACCU host: `ACCU Center → Use Root button` (or auto-detected on launch)
2. Done — uid=0, full privilege

## Architecture Decisions

- **AccuConnectionManager** is `@Singleton` injected everywhere — one connection, all screens share it
- **ShizukuUtils** is kept as a compatibility wrapper only; all its methods delegate to `AccuConnectionManager.exec()`
- **mDNS discovers two service types:** `_adb-tls-pairing._tcp` (pairing port) and `_adb-tls-connect._tcp` (session port). Session port is used for all `adb -s ip:port shell` commands
- **No bundled `adb` binary** — ACCU relies on `adb` being in the device's `$PATH` (available on Android 11+ developer mode devices and all rooted devices)
- **`CrashEntity.shizukuState` field** retained intentionally (DB migration concern) — represents connection state

## Gotchas

- Wireless ADB requires `adb` in `$PATH` on the device. On rooted devices this is always available. On stock Android 11+, `adb` is at `/system/bin/adb` which should be in PATH when developer mode is on.
- The pairing port and session/connection port are DIFFERENT. ACCU auto-discovers both via mDNS — users only see the 6-digit code.
- OTG requires the HOST phone to support USB OTG host mode (not all budget phones do).
- After device reboot, Wireless ADB session must be re-established (`ACCU Center → Restart`), but pairing persists.
- `AccuConnectionManager.checkAndUpdateState()` does NOT detect OTG automatically — call `connectOtg()` explicitly from the UI.

## User Preferences

- Keep `ShizukuUtils` class name — it's an internal compatibility wrapper, intentional
- Keep `GrantMethod.SHIZUKU` enum constant, `shizukuState` DB field — intentional (migration concern)
- Keep `com.accu.ui.shizuku` package name — internal naming, intentional
- Never add Shizuku SDK back as a dependency — ACCU is self-sufficient
- All new privileged features: route through `AccuConnectionManager.exec()`, never `Runtime.exec("su -c ...")`
