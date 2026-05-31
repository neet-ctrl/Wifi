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
- `ShizukuUtils.kt` already existed with its own implementation — never overwrite it.

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
- Navigation: `com/accu/navigation/NavRoutes.kt`, `AppNavigation.kt`
- All features catalog: `com/accu/ui/features/AllFeaturesScreen.kt`
- Services: `com/accu/services/` (InternetTileService, LanguageQSTileService, KeyEventRelayService, AutoFreezeService, FreezeAllTileService, etc.)
- Receivers: `com/accu/receivers/` (ACCDeviceAdminReceiver, ScreenOffReceiver, BootReceiver, etc.)
- Device admin XML: `res/xml/device_admin.xml`
- GitHub Actions: `.github/workflows/build.yml` (debug APK on push/PR)

## formatSize / formatDuration — no duplicate concern
- `formatSize` is public top-level in `AppCleanerScreen.kt` (storage pkg) and `private` in `CallRecorderScreen.kt` (callrecorder pkg) — different packages, no conflict.
- `formatDuration` is public top-level in `InureAnalyticsScreen.kt` and `private` in `CallRecorderScreen.kt` — no conflict.

## InureUsageStatsScreen compilation fixes applied (May 2026)
- Added `import androidx.compose.foundation.text.KeyboardOptions` and `import androidx.compose.ui.text.input.KeyboardType` — use these, not fully-qualified inline refs.
- Replaced `Padding(...)` composable with `PaddingRow(horizontalPadding, topPadding, bottomPadding)` (renamed to avoid conflict with built-in Box padding).
- Replaced `contentPadding = padding + PaddingValues(...)` operator (which needed extension fn) with explicit `PaddingValues(top=..., bottom=..., start=..., end=...)` using `calculateTopPadding()` etc. with `LayoutDirection.Ltr`.

## Dead-code audit status (as of May 2026)
All originally-identified dead buttons/stubs are now fixed:
- DSPControlsScreen: Export/Import fully wired (JSON share-sheet + file picker + regex parser)
- DeduplicatorScreen: FilterChip selectedLocations state hoisted above LazyColumn
- ColorEditorScreen AdvancedTab: buildExportJson lambda passed from parent
- TextEditorScreen: LaunchedEffect reads file; saveFile() writes; "Select All" → clipboard; "Go to Line…" → dialog
- OnlineRulesScreen: All 6 trackers have real package names
- DarkModeScreen: exportSettings() writes JSON to Downloads; import via ActivityResultLauncher

**Why:** These conventions prevent the most common compile errors in this large multi-screen project.
