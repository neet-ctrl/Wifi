# ACCU Integration Checklist

Use this checklist to verify your integration is complete and correct
before shipping. Check off each item in order.

---

## Phase 1: Project Setup

- [ ] **AIDL files copied** — Three files present at exactly these paths:
  - `app/src/main/aidl/com/accu/api/IAccuService.aidl`
  - `app/src/main/aidl/com/accu/api/IAccuPermissionCallback.aidl`
  - `app/src/main/aidl/com/accu/api/IAccuProcessCallback.aidl`

- [ ] **AIDL package declarations** — Each file declares `package com.accu.api;` (not your package)

- [ ] **SDK files copied** — Six files present in your project:
  - `AccuClient.kt`
  - `AccuConstants.kt`
  - `AccuScopes.kt`
  - `AccuPermissionCodes.kt`
  - `AccuConnectionState.kt`
  - `AccuExceptions.kt`

- [ ] **AIDL enabled in Gradle** — `android { buildFeatures { aidl = true } }` present

- [ ] **Coroutines dependency** — `kotlinx-coroutines-android` in `dependencies {}`

- [ ] **minSdk ≥ 29** — `defaultConfig { minSdk = 29 }` or higher

- [ ] **Build succeeds** — `./gradlew assembleDebug` completes without AIDL errors

---

## Phase 2: AndroidManifest

- [ ] **`<queries>` block present** with `<package android:name="com.accu.controlcenter" />`

- [ ] **No AccuSystemService declaration** in your manifest (it lives in ACCU's process)

- [ ] **No extra permissions** added for ACCU (ACCU handles its own permissions)

---

## Phase 3: Connection Lifecycle

- [ ] **AccuClient uses applicationContext** — Not Activity context (to survive rotations)

- [ ] **`connect()` called in ViewModel init** or `Activity.onStart()`

- [ ] **`disconnect()` called in ViewModel.onCleared()** or `Activity.onStop()`

- [ ] **`accuState` observed in UI** — All four states handled:
  - `Idle` — initial state before connect()
  - `Connecting` — show progress indicator
  - `Connected` — show connected UI
  - `Disconnected` — show reconnecting indicator
  - `Error` — show error with reason string

- [ ] **No memory leaks** — ServiceConnection not held past onStop/onCleared

---

## Phase 4: Permission Flow

- [ ] **Permission checked on connect** — `checkPermission()` called after `Connected` state

- [ ] **"Request Permission" button visible** when `permissionCode != PERMISSION_GRANTED`

- [ ] **`requestPermission()` called on button press** — NOT automatically without user action

- [ ] **Permission result handled** — All four codes handled:
  - `PERMISSION_GRANTED (0)` — proceed to use APIs
  - `PERMISSION_DENIED (1)` — show "access denied" message
  - `NOT_YET_REQUESTED (-1)` — show "click to grant" UI
  - `SERVICE_UNAVAILABLE (-2)` — show "ACCU service not running" message

- [ ] **No automatic permission request on launch** — User must initiate (UX policy)

- [ ] **Scope checking before API calls** — `hasScope(scope)` checked where relevant

---

## Phase 5: API Usage

- [ ] **All API calls on Dispatchers.IO** — No ACCU calls on main thread

- [ ] **`exec()` used for short commands** — Commands that complete quickly

- [ ] **`execAsync()` used for long-running commands** — logcat, ping, long builds

- [ ] **`execAsync()` callbacks posted to main thread** if they update UI

- [ ] **Return values checked** — Boolean returns from pkg management methods checked for `false`

- [ ] **`exec()` exit code checked** — `result.isSuccess` or `result.exitCode == 0`

- [ ] **Error handling** — `AccuException` subtypes caught appropriately

---

## Phase 6: Resilience

- [ ] **Handles ACCU not installed** — `AccuConnectionState.Error` shown with helpful message

- [ ] **Handles service not running** — Instructs user to open ACCU → System Service → Enable

- [ ] **Handles service disconnection** — `AccuConnectionState.Disconnected` shown gracefully

- [ ] **Handles scope denied** — `AccuScopeDeniedException` caught and explained to user

- [ ] **Handles permission denied** — `AccuPermissionDeniedException` caught gracefully

---

## Phase 7: Testing

- [ ] **Test on API 30+ device/emulator** — `<queries>` visibility requirement

- [ ] **Test with ACCU service OFF** — Verify error state shown correctly

- [ ] **Test with ACCU service ON, no permission** — Verify dialog appears and result handled

- [ ] **Test user tapping Deny** — Verify denied state handled gracefully

- [ ] **Test user unchecking some scopes** — Verify scope-denied handling works

- [ ] **Test ACCU force-stop** — Kill ACCU, verify auto-reconnect works

- [ ] **Test device rotation** — Verify ViewModel survives, no double-bind

- [ ] **Test app restart** — Verify permission is remembered (no second dialog)

- [ ] **Test `exec()` with bad command** — Verify exit code != 0 handled

- [ ] **Test `execAsync()` early disconnect** — Kill ACCU mid-stream, no crash

---

## Phase 8: Code Quality

- [ ] **No hardcoded package strings** — Use `AccuConstants.ACCU_PACKAGE` everywhere

- [ ] **No hardcoded permission codes** — Use `AccuConstants.PERMISSION_*` everywhere

- [ ] **No hardcoded scope strings** — Use `AccuScopes.*` everywhere

- [ ] **AccuClient not recreated on every recomposition** — Stored in ViewModel, not Composable

- [ ] **No `runBlocking` wrapping ACCU calls** — Use proper coroutine scopes

---

## Phase 9: User-Facing Requirements

- [ ] **App discloses ACCU requirement** — App description, onboarding, or help screen

- [ ] **"ACCU not installed" screen** with installation instructions

- [ ] **"ACCU service not running" screen** with steps to enable it

- [ ] **Permission rationale shown** before triggering `requestPermission()` (best practice)

- [ ] **Graceful degradation** — Core app features work without ACCU; privileged features
      are clearly marked as requiring ACCU

---

## Quick Verification Commands

```bash
# 1. Verify AIDL compiles
./gradlew generateDebugAidlInterfaces

# 2. Verify full build
./gradlew assembleDebug

# 3. Check binding at runtime (install on device with ACCU running)
adb shell dumpsys activity services com.accu.controlcenter

# 4. Watch ACCU logs
adb logcat -s "ACCU" -s "AccuSystemService" -s "AccuServiceImpl"

# 5. Verify your app is listed in ACCU's connected apps
# Open ACCU → System Service → Connected Apps
```
