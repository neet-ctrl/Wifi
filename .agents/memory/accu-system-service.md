---
name: ACCU System Service Architecture
description: Full details on the IPC privilege broker — the Shizuku replica built into ACCU.
---

# ACCU System Service

## Overview
A Shizuku-replica privilege broker. Third-party apps bind to ACCU's exported service and get full ADB/shell-level privileges through a user-controlled permission model.

## AIDL — `app/src/main/aidl/com/accu/api/`
- `IAccuService.aidl` — 75-method binder interface (transaction IDs 1–75, stable)
- `IAccuPermissionCallback.aidl` — oneway; `onPermissionResult(int)` codes: 0=GRANTED, 1=DENIED, -1=NOT_REQUESTED, -2=ERROR
- `IAccuProcessCallback.aidl` — oneway streaming: `onStdoutLine`, `onStderrLine`, `onExit`

## Service files — `app/src/main/java/com/accu/service/`
- `AccuPermissionManager.kt` — `@Singleton`; stores grants as JSON in SharedPreferences key `grants_v1`. Exposes `StateFlow<List<AccuClientGrant>>`. Scopes: SHELL, PACKAGE_MANAGE, PERMISSIONS, SETTINGS, LOCALE, ALL.
- `AccuServiceImpl.kt` — `IAccuService.Stub`; uses `Binder.getCallingUid()` → `getPackagesForUid()` to identify callers; calls `requireScope()` before every privileged method.
- `AccuSystemService.kt` — `@AndroidEntryPoint` foreground service; binding action: `com.accu.api.AccuSystemService`; package: `com.accu.controlcenter`. Exposes `companion object` `StateFlow` for `isRunning` and `pendingRequests` (observed by ViewModel). Handles `ACTION_GRANT` / `ACTION_DENY` intents for notification quick-actions.
- `AccuPermissionRequestActivity.kt` — Bottom-sheet style; shown via `startActivity()` from the service when a new permission request arrives. Calls `serviceInstance?.grantFromActivity()` or `denyFromActivity()` directly (same-process cast via reflection on the Binder).

## UI files — `app/src/main/java/com/accu/ui/apiservice/`
- `AccuServiceViewModel.kt` — `@HiltViewModel`; observes `AccuSystemService.*` flows + `permissionManager.grants`.
- `AccuServiceScreen.kt` — 3-tab UI: Apps (connected), Pending, SDK Docs quick-ref. Status card with start/stop button.
- `AccuSdkDocsScreen.kt` — 10 collapsible sections; full developer guide including AccuClient wrapper class, all 75 API methods, Gradle AIDL setup, error handling. All code blocks have copy buttons.

## Navigation
- Route `accu_service_hub` → `Screen.AccuServiceHub`
- Route `accu_sdk_docs` → `Screen.AccuSdkDocs`
- Entry point: ShizukuCenterScreen top-bar `Api` icon → `onNavigateToAccuServiceHub`
- Also reachable via global search ("ACCU Service Hub", "ACCU SDK Docs")

## Manifest
- Service registered with `android:exported="true"`, intent-filter action `com.accu.api.AccuSystemService`, `foregroundServiceType="specialUse"`, protected by `com.accu.api.permission.BIND_ACCU_SERVICE` (normal protectionLevel — client apps just declare `<uses-permission>`).
- `AccuPermissionRequestActivity` registered with `launchMode="singleTop"`, not exported.
- `FOREGROUND_SERVICE_CONNECTED_DEVICE` permission declared.

## Client binding (for third-party apps)
```kotlin
val intent = Intent("com.accu.api.AccuSystemService")
    .setPackage("com.accu.controlcenter")
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
// onServiceConnected → IAccuService.Stub.asInterface(binder)
```
Client must also declare in manifest:
```xml
<queries><package android:name="com.accu.controlcenter" /></queries>
<uses-permission android:name="com.accu.api.permission.BIND_ACCU_SERVICE"/>
```

**Why:** AIDL package `com.accu.api` and transaction IDs must never change — breaking them breaks all existing clients. Always add new methods at the end with the next unused ID.
