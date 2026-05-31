<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="ACCU Logo" />

# Android Control Center Ultimate

### One app to rule them all — 17 open-source tools merged into a single Material 3 powerhouse

[![Build](https://img.shields.io/github/actions/workflow/status/your-org/android-control-center/build.yml?branch=main&style=for-the-badge&logo=github&logoColor=white)](../../actions)
[![Version](https://img.shields.io/badge/version-1.0.0-6750A4?style=for-the-badge&logo=android&logoColor=white)](../../releases)
[![API](https://img.shields.io/badge/API-29%2B%20(Android%2010)-34A853?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/about/versions/10)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-EA4335?style=for-the-badge)](LICENSE)

</div>

---

## What is ACCU?

**Android Control Center Ultimate** consolidates **17 best-in-class open-source tools** into a single unified app — one install, one UI language, zero redundancy. Every feature is re-implemented in modern **Kotlin + Jetpack Compose** with a **Material 3 Expressive** design system.

> **No Shizuku required.** ACCU is fully self-sufficient via its own `AccuConnectionManager` — one global privilege broker that serves every feature.

---

## How Privilege Works — AccuConnectionManager

Every feature in ACCU routes through a single `@Singleton` called **`AccuConnectionManager`**. There is no separate Shizuku app, no external dependency. Users connect **once**, and every screen automatically benefits.

```
Shell Terminal
App Freeze          ╮
Debloater           │
Permission Manager  ├──▶  AccuConnectionManager.exec("command")
Component Manager   │           │
File Manager        │           ├── Root (LibSU)          [preferred]
QS Tiles            │           ├── Wireless ADB          [adb -s ip:port shell]
Dark Mode           │           ├── OTG / USB ADB         [adb shell]
Language Selector   ╯           └── Plain shell           [unprivileged fallback]
```

**Priority order:** Root → Wireless ADB → OTG ADB → Plain shell. The highest-available method wins automatically on every `exec()` call.

---

## Connecting ACCU — Full Step-by-Step

### Where to find the connection UI

Open ACCU → **ACCU Center** (shield icon, bottom navigation or dashboard card).

The status card at the top shows:
- 🟢 **ACCU Connected** — privilege active, method shown (Root / Wireless ADB / OTG)
- 🔴 **Not Connected** — three connection buttons visible

---

### Method 1: Root (Automatic — no setup)

If the device is rooted, ACCU detects root automatically on first launch via LibSU.

```
ACCU Center → tap "Use Root"
→ Root shell opens instantly
→ Status card: "ACCU Connected · Root · uid=0"
```

All features immediately work at full privilege. No further steps needed.

---

### Method 2: Wireless ADB (Android 11+ — recommended for non-rooted devices)

**Requirements:** Both phones on the same Wi-Fi network. Target phone: Android 11+.

#### Step 1 — Enable Developer Options on the target phone
```
Settings → About phone → tap "Build number" 7 times
→ "You are now a developer!"
```
> Samsung: Settings → About phone → Software information → Build number
> MIUI/HyperOS: Settings → About phone → tap MIUI version 7 times

#### Step 2 — Enable Wireless Debugging on the target phone
```
Settings → Developer Options → Wireless debugging → toggle ON
```
> Both phones must be on the same Wi-Fi network (or one hosts a hotspot the other joins).

#### Step 3 — Get the pairing code from the target phone
```
Settings → Developer Options → Wireless debugging
→ tap "Pair device with pairing code"
→ Note the 6-digit code and the pairing port shown
   Example: "Wi-Fi pairing code: 123456  Port: 37839"
```

#### Step 4 — Start discovery in ACCU on the host phone
```
ACCU → ACCU Center → tap "Wireless ADB" button
→ ACCU starts mDNS auto-discovery
→ When target is found, a notification appears:
  "Wireless Debugging Detected — Open ACCU to enter code"
→ Or: ACCU Center shows "Enter your 6-digit pairing code"
```

#### Step 5 — Enter the 6-digit code
```
ACCU Center → Pairing step → enter the 6-digit code → tap Confirm
→ ACCU runs: adb pair <ip>:<pairing_port> <code>
→ Then auto-connects: adb connect <ip>:<session_port>
→ Status card: "ACCU Connected · Wireless ADB (192.168.x.x) · uid=2000"
```

**That's it.** Every feature in the app now routes through the wireless ADB session automatically.

#### Notes on pairing vs connection ports
| Port | What it is | Where you see it |
|------|-----------|-----------------|
| **Pairing port** | Used only once to pair | Shown in the "Pair device with pairing code" dialog |
| **Session/connection port** | Used for all commands | Shown on the main Wireless debugging page |

ACCU auto-discovers both via mDNS — you only ever need to type the 6-digit code.

#### Reconnecting after a restart
```
ACCU Center → tap "Restart" (if shown)
→ ACCU runs: adb connect <last_ip>:<last_port>
→ Reconnects in ~1 second without re-pairing
```
Pairing persists across reboots on the same Wi-Fi network. Only the session needs to be re-established.

---

### Method 3: OTG / USB ADB (phone-to-phone, no Wi-Fi needed)

**Requirements:**
- **Host phone** (running ACCU): must support USB OTG host mode
- **Target phone** (to be controlled): USB Debugging enabled
- **Cable:** USB-C OTG adapter + USB-C cable, or a USB-C to USB-C OTG cable

#### Step 1 — Enable USB Debugging on the target phone
```
Settings → About phone → Build number (tap 7×)
Settings → Developer Options → USB debugging → ON
```

#### Step 2 — Connect the phones
```
HOST phone ←[USB-OTG adapter]←[USB cable]→ TARGET phone
```
> If using a USB-C to USB-C cable, one end must be OTG host mode. Most USB-C to USB-C OTG cables auto-negotiate.

#### Step 3 — Tap "OTG / USB" in ACCU
```
ACCU → ACCU Center → tap "OTG / USB" button
→ ACCU runs: adb devices (checks for USB-connected device)
```

#### Step 4 — Approve on the target phone
```
[Target phone shows dialog]
"Allow USB debugging?
 RSA key fingerprint: AB:CD:EF:..."
→ Tick "Always allow from this computer"
→ Tap Allow
```

#### Step 5 — Connected
```
ACCU Center status: "ACCU Connected · OTG / USB ADB · uid=2000"
→ All features in ACCU now run on the target phone
```

> **Tip:** Once OTG is connected, switch to wireless so you can remove the cable:
> ```
> ACCU Shell → run: adb tcpip 5555
> Get target IP: adb shell ip route
> → Then use Wireless ADB method above
> ```

---

## How All 85+ Features Use the Same Connection

Every ViewModel, TileService, and background service injects `AccuConnectionManager` via Hilt. The call path is identical everywhere:

```kotlin
// In any ViewModel / TileService:
@Inject lateinit var connectionManager: AccuConnectionManager
// or via:
@Inject lateinit var shizukuUtils: ShizukuUtils   // thin wrapper

// Then just:
val result = connectionManager.exec("pm suspend --user 0 com.some.app")
// ↑ Automatically uses root / wireless ADB / OTG — whichever is active
```

You **never** need to re-connect when switching screens. The singleton state persists for the entire app session. QS tiles, background workers, and accessibility services all share the same connection.

---

## Feature Overview

| # | Feature Area | Source Project | What You Get |
|---|---|---|---|
| 1 | **ACCU Center** | Built-in | Wireless ADB auto-pair, OTG connect, root detect, diagnostics, per-app grants |
| 2 | **Interactive Shell** | [aShellYou](https://github.com/DP-Hridayan/aShellYou) | Full ADB shell with history, syntax highlighting, AI analysis, command examples |
| 3 | **App Debloat** | [Canta](https://github.com/samolego/Canta) | Safe uninstall/disable of system & carrier bloatware, community presets, undo logs |
| 4 | **App Freeze** | [Hail](https://github.com/aistra0528/Hail) | Suspend apps via ADB (no root), freeze on screen-off, scheduled auto-freeze, QS tile |
| 5 | **App Inspector** | [Inure](https://github.com/Hamza417/Inure) | Deep app info: storage breakdown, permissions, activities, services, trackers, VirusTotal |
| 6 | **Component Manager** | [Blocker](https://github.com/lihenggui/blocker) | Enable/disable Activities, Services, Receivers, Providers; IFW rule import/export |
| 7 | **Monet Theming** | [ColorBlendr](https://github.com/Mahmud0808/ColorBlendr) | Material You color palette editor, seed color picker, Monet style selector |
| 8 | **Dark Mode** | [DarQ](https://github.com/KieronQuinn/DarQ) | Per-app forced dark mode, geolocation scheduling, sunrise/sunset automation |
| 9 | **Widgets** | [SmartSpacer](https://github.com/KieronQuinn/SmartSpacer) | Lock-screen and notification bar widget management, complication targets |
| 10 | **Storage & Cleanup** | [SD Maid SE](https://github.com/d4rken-org/sdmaid-se) | Junk cleaner, duplicate finder, large-file browser, corpse finder |
| 11 | **File Manager** | [Material Files](https://github.com/zhanghai/MaterialFiles) | Root-capable file browser, archive support, SMB/FTP server, file properties |
| 12 | **APK Installer** | [InstallWithOptions](https://github.com/zacharee/InstallWithOptions) | Install APKs with downgrade, test-package, grant-all-permissions |
| 13 | **Key Mapper** | [Key Mapper](https://github.com/keymapperorg/KeyMapper) | Remap hardware buttons, gestures, volume keys to any action |
| 14 | **Language Selector** | [Language Selector](https://github.com/VegaBobo/Language-Selector) | Per-app language overrides, system locale switcher |
| 15 | **Internet Tiles** | [BetterInternetTiles](https://github.com/CasperVerswijvelt/Better-Internet-Tiles) | Wi-Fi, Mobile Data, Hotspot, NFC, Bluetooth, Airplane Mode QS tiles |
| 16 | **Audio DSP** | [RootlessJamesDSP](https://github.com/ThePBone/RootlessJamesDSP) | System-wide EQ, bass boost, stereo widening, reverb, convolver, LiveProg |
| 17 | **Call Recording** | [ShizuCallRecorder](https://github.com/chenxiaolong/BCR) | Rootless call recording, playback, auto-export |

---

## Highlights

- **Global Command Palette** — search all 85+ screens from anywhere, fuzzy match, Quick Launch tiles, Recently Visited row
- **Notification Center** — 11 channels, snooze controls, per-channel preferences
- **Live Dashboard** — real-time RAM, storage, battery, CPU; ACCU connection health card
- **Material 3 Expressive** — glassmorphism surfaces, spring animations, adaptive navigation
- **Zero telemetry** — no analytics, no network calls home, fully on-device
- **ACCU System Service** — third-party IPC broker with full AIDL API, scope-based permissions, boot autostart

---

## ACCU System Service — Third-Party Developer API

ACCU acts as a **privileged IPC broker** for other apps via its AIDL interface. No external SDK required by clients.

```
Your App  →  bindService("com.accu.api.AccuSystemService")  →  AccuSystemService
                                                                      ↓
                                                             AccuPermissionManager
                                                             (user-controlled grants)
                                                                      ↓
                                                             AccuConnectionManager
                                                             (root / wireless ADB / OTG)
                                                                      ↓
                                                             Android System APIs
```

```kotlin
val intent = Intent("com.accu.api.AccuSystemService").setPackage("com.accu.controlcenter")
context.bindService(intent, connection, BIND_AUTO_CREATE)

// In onServiceConnected:
val accu = IAccuService.Stub.asInterface(binder)
accu.requestPermission(callback)
val result = accu.exec("pm list packages")
```

Full guide: `docs/ACCU_THIRD_PARTY_DEVELOPER_GUIDE.md`

---

## Requirements

| Requirement | Details |
|---|---|
| **Android version** | 10.0+ (API 29) |
| **Privilege (choose one)** | Root OR Android 11+ Wireless Debugging OR USB OTG host mode |
| **No external app needed** | ACCU is fully self-sufficient — no Shizuku, no ADB tools on PC |

---

## Building

```bash
# Clone
git clone https://github.com/your-org/android-control-center.git
cd android-control-center/android-control-center

# Debug build
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Install to connected device
./gradlew installDebug

# Release build (set signing env vars first)
./gradlew assembleRelease
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  Jetpack Compose · Material 3 · MVVM ViewModels         │
│  85+ screens · 5 bottom tabs · Command Palette          │
└──────────────────────────┬──────────────────────────────┘
                           │ StateFlow / collectAsState
┌──────────────────────────▼──────────────────────────────┐
│                    Domain Layer                         │
│         Use Cases · Business Logic · Models             │
└──────────┬────────────────────────┬────────────────────-┘
           │                        │
┌──────────▼───────────┐  ┌─────────▼──────────────────────┐
│    Data Layer         │  │  Privilege Layer (ACCU-native)  │
│  Room DB (16 tables) │  │  AccuConnectionManager          │
│  DataStore Prefs     │  │  ├── LibSU root shell           │
│  Repositories        │  │  ├── Wireless ADB (mDNS)        │
└──────────────────────┘  │  └── OTG / USB ADB              │
                          └────────────────────────────────┘
```

### Key Design Decisions

- **Single Activity** — `MainActivity` hosts the entire nav graph; edge-to-edge with `WindowCompat`
- **Hilt DI** — all ViewModels, repositories, services are Hilt-injected; `@Singleton` shared across the graph
- **AccuConnectionManager singleton** — all 85+ screens share one connection; root/wireless/OTG auto-selected
- **Contract-first connection** — mDNS discovers pairing port AND session port; user only types 6-digit code
- **DataStore** — navigation history, notification prefs, user settings use `androidx.datastore.preferences`

---

## Project Structure

```
app/src/main/java/com/accu/
├── connection/
│   └── AccuConnectionManager.kt  — global privilege singleton (root / wireless ADB / OTG)
├── utils/
│   └── ShizukuUtils.kt           — thin wrapper delegating to AccuConnectionManager
│
├── ui/shizuku/
│   ├── ShizukuCenterScreen.kt    — ACCU Center: connection status, Wireless/OTG/Root buttons
│   ├── ShizukuViewModel.kt       — connection management, diagnostics, authorized apps
│   └── AdbPairingScreen.kt       — guided 4-step wireless ADB pairing wizard
│
├── services/                     — QS tiles (all inject AccuConnectionManager via Hilt)
└── ui/                           — 85+ screens, all share the same connection
```

---

## Privacy

- **No telemetry** — zero analytics SDKs, no crash reporters phoning home
- **No network calls** — all operations are local, on-device only
- **Secure backup** — sensitive session data excluded from cloud backup

---

## Contributing

1. Fork, create branch: `git checkout -b feat/your-feature`
2. All new screens must be added to `SearchIndex.kt`
3. New privileged operations: use `connectionManager.exec()`, never `Runtime.exec("su -c ...")`
4. New TileServices: use `@AndroidEntryPoint` + `@Inject lateinit var connectionManager`
5. Submit PR against `main`

---

## Acknowledgements

[aShellYou](https://github.com/DP-Hridayan/aShellYou) · [BetterInternetTiles](https://github.com/CasperVerswijvelt/Better-Internet-Tiles) · [Blocker](https://github.com/lihenggui/blocker) · [Canta](https://github.com/samolego/Canta) · [ColorBlendr](https://github.com/Mahmud0808/ColorBlendr) · [DarQ](https://github.com/KieronQuinn/DarQ) · [Hail](https://github.com/aistra0528/Hail) · [InstallWithOptions](https://github.com/zacharee/InstallWithOptions) · [Inure](https://github.com/Hamza417/Inure) · [Key Mapper](https://github.com/keymapperorg/KeyMapper) · [Language Selector](https://github.com/VegaBobo/Language-Selector) · [Material Files](https://github.com/zhanghai/MaterialFiles) · [RootlessJamesDSP](https://github.com/ThePBone/RootlessJamesDSP) · [SD Maid SE](https://github.com/d4rken-org/sdmaid-se) · [ShizuCallRecorder](https://github.com/chenxiaolong/BCR) · [SmartSpacer](https://github.com/KieronQuinn/SmartSpacer)

---

## License

```
Copyright 2025 ACCU Contributors
Licensed under the Apache License, Version 2.0
```

<div align="center">Made with Kotlin · Compose · Material 3</div>
