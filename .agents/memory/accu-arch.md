---
name: ACCU Project Architecture
description: Android Control Center Ultimate — key structure, conventions, and gotchas for future sessions.
---

## Project root
`/home/runner/workspace/android-control-center/`
Package: `com.accu.controlcenter` (applicationId), code package `com.accu`
minSdk 29, targetSdk/compileSdk 36

## Scale (as of May 2026)
171 Kotlin files, ~45,300 lines, 103 Screen routes (NavRoutes ↔ AppNavigation perfectly symmetric)

## Critical conventions
- `NavRoutes.kt` — ALL screen destinations; add new routes here first.
- `AppNavigation.kt` — ONE composable() per route; imports must match package + function name, NOT file name.
- `FileManagerAdvancedFeaturesScreen` lives in `MaterialFilesAdvancedScreen.kt` → import as `FileManagerAdvancedFeaturesScreen`.
- `CantaPresetsScreen.onApplyPreset` takes `(DebloatPreset) -> Unit`, not `() -> Unit`.
- `StorageScreen` uses Hilt ViewModel — always include `viewModel: StorageViewModel = hiltViewModel()` last.
- `ShizukuUtils.kt` is a THIN WRAPPER over `AccuConnectionManager` — never replace or remove it.
- All new privileged operations: `connectionManager.exec("command")`, NEVER `Runtime.getRuntime().exec(arrayOf("su", "-c", ...))`.
- All new QS TileServices: `@AndroidEntryPoint` + `@Inject lateinit var connectionManager: AccuConnectionManager`.

## Icon rules (critical)
- `material-icons-extended` is in `gradle/libs.versions.toml` (alias `compose-material-icons`) → virtually all Material Icons available.
- `Icons.Default.Tile` does NOT exist → use `Icons.Default.ViewDay` for QS tile icons.
- `Icons.Outlined.ViewDay` does NOT exist → use `Icons.Default.ViewDay`.
- Starred safe list of less-common icons verified present: `FlashlightOn`, `Screenshot`, `NetworkCell`, `BatteryChargingFull`, `Bluetooth`, `Pause`, `Timer`, `WarningAmber`, `CheckCircle`, `SwipeRight`, `SwipeUp`, `WbCloudy`, `WbTwilight`, `SelfImprovement`, `ManageSearch`, `NightlightRound`, `BlurCircular`, `NavigationRounded`, `HistoryToggleOff`, `PauseCircleOutline`, `SettingsSystemDaydream`, `ScreenLockPortrait`, `StayCurrentPortrait`, `StayCurrentLandscape`, `BlockFlipped`, `DialerSip`, `BrandingWatermark`, `GppBad`, `TrackChanges`, `MonitorHeart`, `SearchOff`, `AppsOutage`, `QrCodeScanner`, `LockClock`, `WavingHand`, `Thermostat`, `TravelExplore`.

## Source repos covered (all 17)
Shizuku, aShellYou, Canta, Hail, Inure, Blocker, ColorBlendr, DarQ, SmartSpacer,
SD Maid SE, Material Files, InstallWithOptions, Key Mapper, Language Selector,
Better Internet Tiles, RootlessJamesDSP, ShizuCallRecorder

## Key file locations
- **Privilege singleton**: `com/accu/connection/AccuConnectionManager.kt`
- **Privilege wrapper**: `com/accu/utils/ShizukuUtils.kt` (thin delegate to AccuConnectionManager)
- **Connection UI**: `com/accu/ui/shizuku/ShizukuCenterScreen.kt` (Wireless/OTG/Root buttons)
- **Connection ViewModel**: `com/accu/ui/shizuku/ShizukuViewModel.kt`
- **Pairing wizard**: `com/accu/ui/shizuku/AdbPairingScreen.kt`
- Navigation: `com/accu/navigation/NavRoutes.kt`, `AppNavigation.kt`
- All features catalog: `com/accu/ui/features/AllFeaturesScreen.kt`
- Services: `com/accu/services/` (all QS tiles inject AccuConnectionManager via @AndroidEntryPoint)
- Receivers: `com/accu/receivers/` (ACCDeviceAdminReceiver, ScreenOffReceiver, BootReceiver, etc.)
- Device admin XML: `res/xml/device_admin.xml`
- GitHub Actions: `.github/workflows/build.yml` (debug APK on push/PR)

## AccuConnectionManager connection states
- `DISCONNECTED` — no privilege, plain shell
- `DISCOVERING` — mDNS pairing scan running
- `AWAITING_CODE` — pairing service found, user must enter 6-digit code
- `CONNECTING` — running `adb pair` + `adb connect`
- `CONNECTED_WIRELESS` — wireless ADB session active; exec routes via `adb -s ip:port shell`
- `CONNECTED_ROOT` — LibSU root available; exec routes via `Shell.cmd()`
- `CONNECTED_OTG` — USB OTG device detected; exec routes via `adb shell`

## Connection button locations in UI
- `ShizukuCenterScreen.kt` action row (line ~185): "Wireless ADB", "OTG / USB", "Use Root" buttons — visible when `!state.isAvailable`
- "Restart" and "Stop" buttons — visible when `state.isAvailable`
- All three connection state checks (`isConnected`) in `ShizukuViewModel` include ROOT + WIRELESS + OTG

## isConnected pattern (all three states)
```kotlin
val isConnected = connState == AccuConnectionManager.ConnectionState.CONNECTED_ROOT
        || connState == AccuConnectionManager.ConnectionState.CONNECTED_WIRELESS
        || connState == AccuConnectionManager.ConnectionState.CONNECTED_OTG
```
Must always check all three. Never check only ROOT + WIRELESS.

## mDNS — two service types discovered
- `_adb-tls-pairing._tcp` → pairing port (used once for `adb pair`)
- `_adb-tls-connect._tcp` → session port (used for all `adb -s ip:port shell` commands)
Both auto-discovered; `sessionPort` saved to SharedPrefs as `last_adb_port` after connect.

## formatSize / formatDuration — no duplicate concern
- `formatSize` is public top-level in `AppCleanerScreen.kt` (storage pkg) and `private` in `CallRecorderScreen.kt` (callrecorder pkg) — different packages, no conflict.
- `formatDuration` is public top-level in `InureAnalyticsScreen.kt` and `private` in `CallRecorderScreen.kt` — no conflict.

## Dead-code audit status (as of May 2026)
All originally-identified dead buttons/stubs are fixed. All privileged calls route through AccuConnectionManager.

**Why:** These conventions prevent the most common compile errors and privilege-bypass bugs in this large multi-screen project.
