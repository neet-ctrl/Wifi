# ACCU Architecture

## IPC Call Flow

```
Your App (any APK)
       │
       │  bindService("com.accu.api.AccuSystemService",
       │               package="com.accu.controlcenter")
       ▼
  AccuClient.kt  ─────── (SDK helper in YOUR app)
       │
       │  Android Binder IPC (cross-process)
       ▼
  IAccuService.Stub  ─── (generated from IAccuService.aidl)
       │
       ▼
  AccuServiceImpl.kt ─── (runs inside ACCU's process)
       │
       ├── requireScope()  ──▶  AccuPermissionManager.kt
       │                          (reads from SharedPreferences:
       │                           "accu_api_grants")
       │
       └── runCmd()  ──▶  Runtime.getRuntime().exec(["sh", "-c", cmd])
                           │
                           ▼
                    Android System APIs
                    (pm, am, settings, appops)
                           │
                           ▼
                    Android OS / Kernel
```

## Component Roles

| Component | Location | Role |
|---|---|---|
| `IAccuService.aidl` | YOUR app + ACCU | Defines the IPC contract. Must be identical in both apps. |
| `IAccuPermissionCallback.aidl` | YOUR app + ACCU | One-shot callback for permission result. |
| `IAccuProcessCallback.aidl` | YOUR app + ACCU | Streaming callback for async shell output. |
| `AccuClient.kt` | YOUR app (SDK) | Manages binding lifecycle, wraps raw binder. |
| `AccuSystemService.kt` | ACCU app | Android Service that exposes the binder. Foreground service. |
| `AccuServiceImpl.kt` | ACCU app | Implements IAccuService.Stub. Validates permissions, runs commands. |
| `AccuPermissionManager.kt` | ACCU app | Stores grant/deny decisions per package in SharedPreferences. |
| `AccuPermissionRequestActivity.kt` | ACCU app | Full-screen bottom-sheet dialog shown to the user on first bind. |

## Permission Flow (First Connection)

```
Your App                    AccuSystemService           ACCU UI
    │                              │                       │
    │── requestPermission() ──────▶│                       │
    │                              │── launch dialog ─────▶│
    │                              │                       │
    │                              │◀─── user taps Grant ──│
    │                              │                       │
    │                              │  persist grant to     │
    │                              │  SharedPreferences    │
    │                              │                       │
    │◀─ onPermissionResult(0) ─────│                       │
    │   (PERMISSION_GRANTED)       │                       │
    │                              │                       │
    │── exec("id") ───────────────▶│                       │
    │                              │ requireScope(SHELL) ✅ │
    │                              │ runCmd("sh -c id")    │
    │◀─ ["uid=0...", "", "0"] ─────│                       │
```

## Permission Flow (Subsequent Connections)

On subsequent app launches, ACCU remembers the grant. No dialog is shown:

```
Your App                    AccuSystemService
    │                              │
    │── bindService() ────────────▶│
    │◀─ onServiceConnected() ──────│
    │                              │
    │── checkPermission() ────────▶│  reads SharedPreferences
    │◀─ 0 (PERMISSION_GRANTED) ───│  (instant, no dialog)
    │                              │
    │── exec("whatever") ─────────▶│  executes immediately
    │◀─ result ────────────────────│
```

## Scope System

ACCU uses a fine-grained scope model. When the user grants permission,
they can limit which categories of APIs your app can call.

```
ACCU Permission Dialog
┌─────────────────────────────────────────────┐
│ [App Icon]  My App                          │
│             com.example.myapp               │
│                                             │
│ ⚠ ACCU Privilege Request                   │
│                                             │
│ Requested access:                           │
│ [>] Shell Commands        [toggle ON/OFF]   │
│ [>] Package Management    [toggle ON/OFF]   │
│ [>] Permissions           [toggle ON/OFF]   │
│ [>] System Settings       [toggle ON/OFF]   │
│ [>] Locale Control        [toggle ON/OFF]   │
│                                             │
│  [  Deny  ]    [  Grant 5 Scopes  ]         │
└─────────────────────────────────────────────┘
```

The user can turn off individual scopes before granting.
If your app calls an API whose scope was disabled, ACCU throws
a `SecurityException` and AccuClient propagates it.

## Key Difference From Shizuku

| Feature | Shizuku | ACCU |
|---|---|---|
| Backend | Shizuku's own privileged daemon | Shizuku or root (ACCU uses Shizuku internally) |
| Permission UX | Generic "allow" dialog | Rich scope-selector bottom sheet with explanations |
| Scope system | App-level on/off | Fine-grained 5 scopes per app |
| API shape | Raw binder, no helpers | Full Kotlin SDK with AccuClient wrapper |
| Streaming shell | No built-in | `execAsync()` + `IAccuProcessCallback` |
| Locale API | Not available | `setApplicationLocale()` built-in |
| Error types | DeadObjectException | Typed AccuException hierarchy |

## Data Persistence

ACCU stores all grants in SharedPreferences on ACCU's own process:

```
File: /data/data/com.accu.controlcenter/shared_prefs/accu_api_grants.xml
Key: grants_v1
Value: JSON array of AccuClientGrant objects

Schema per grant:
{
  "pkg":       "com.example.yourapp",
  "label":     "Your App",
  "scopes":    ["SHELL", "PACKAGE_MANAGE", "PERMISSIONS", "SETTINGS", "LOCALE"],
  "grantedAt": 1748000000000,
  "lastUsed":  1748100000000,
  "granted":   true,
  "calls":     47
}
```

Grants survive ACCU updates. They are only cleared if:
- The user explicitly revokes access in ACCU → System Service screen
- Your app calls `IAccuService.revokeSelf()`
- ACCU is uninstalled (obviously)
