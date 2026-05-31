---
name: Shizuku Removal — ACCU Self-Sufficient Privilege
description: Full removal of rikka.shizuku library; ACCU now owns its own IPC privilege via AccuConnectionManager.
---

## The Rule
ACCU has zero dependency on `rikka.shizuku`. All privilege flows through `AccuConnectionManager` (package `com.accu.connection`).

**Why:** User requirement — ACCU IS its own privilege broker. One global connection shared by all features, not per-screen.

**How to apply:** Any new feature needing privilege must inject `AccuConnectionManager` or `ShizukuUtils` (which delegates to it). Never add back `rikka.shizuku`.

## Architecture

### AccuConnectionManager (singleton @Inject)
- `ConnectionState` enum: DISCONNECTED → DISCOVERING → AWAITING_CODE → CONNECTING → CONNECTED_WIRELESS / CONNECTED_ROOT
- `isPrivilegeAvailable()` — root or wireless ADB
- `exec(command)` — suspend, routes: LibSU root → wireless ADB → plain shell
- `startPairingDiscovery()` / `stopPairingDiscovery()` — NsdManager, discovers `_adb-tls-pairing._tcp`
- `completePairing(code)` — user enters ONLY the 6-digit code; IP+port auto-grabbed from mDNS
- Shows notification when pairing service detected (like Shizuku's flow)
- Persists last IP in SharedPrefs (`accu_connection_prefs`)

### ShellResult
Defined in `com.accu.connection.AccuConnectionManager` (same file, end of file).
ShizukuUtils imports it from there. Do NOT define it elsewhere.

### ShizukuUtils (keeps old method names for 50+ call sites)
- `isShizukuAvailable()` → `connectionManager.isPrivilegeAvailable()`
- `isShizukuInstalled()` → always true (ACCU is always "installed")
- `execShizuku(cmd)` → `connectionManager.exec(cmd)` (suspend)
- `execAdb(cmd)` → `connectionManager.execPlainShell(cmd)`

### ShizukuViewModel (kept same class name, nav routes unchanged)
- Observes `connectionManager.state` flow instead of Shizuku binder listeners
- `startWithAdb()` → `connectionManager.startPairingDiscovery()`
- `startWithRoot()` → test LibSU, then checkAndUpdateState()
- `completePairing(code)` → `connectionManager.completePairing(code)` — only code arg needed

## Notification Channel
Old: `AccuChannels.SHIZUKU_SERVICE = "shizuku_service"`
New: `AccuChannels.ACCU_CONNECTION = AccuConnectionManager.CHANNEL_ID = "accu_connection"`
Channel defined/created in `ACCApplication.createNotificationChannels()`.

## What Was Removed
- `libs.shizuku.api` + `libs.shizuku.provider` from `build.gradle.kts`
- `<provider android:name="rikka.shizuku.ShizukuProvider">` from `AndroidManifest.xml`
- All `import rikka.shizuku.*` from 14 files

## Files Changed
AccuConnectionManager.kt (NEW), ShizukuUtils.kt, AutoFreezeService.kt, FreezeAllTileService.kt, AutoFreezeWorker.kt, AccuSystemService.kt, AccuServiceImpl.kt, ShizukuViewModel.kt, AdbPairingScreen.kt, ShizukuCenterScreen.kt, ShizukuAppsScreen.kt, InstallerScreen.kt, ShizukuUserService.kt (gutted), ACCApplication.kt, AccuNotificationHelper.kt, DashboardViewModel.kt, build.gradle.kts, AndroidManifest.xml.
