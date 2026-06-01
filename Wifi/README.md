# AirKey — WiFi QR Code Manager

<div align="center">

<img src="wifi-qr-app/app/src/main/res/drawable/splash_icon.xml" width="96" alt="AirKey Logo"/>

**Scan any WiFi QR code from any phone or app. Instantly see all details. Generate designer-level custom QR codes. Save networks forever.**

![Android](https://img.shields.io/badge/Android-26%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-blue)
![Room](https://img.shields.io/badge/Room-2.6.1-orange)
![ML Kit](https://img.shields.io/badge/ML%20Kit-Barcode%20Scanning-red?logo=google)
![GitHub Actions](https://img.shields.io/badge/CI-GitHub%20Actions-black?logo=github)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

</div>

---

## Table of Contents

- [What Is AirKey?](#what-is-airkey)
- [Features — Full List](#features--full-list)
- [Screenshots & UI Tour](#screenshots--ui-tour)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [How to Build](#how-to-build)
- [GitHub Actions CI/CD](#github-actions-cicd)
- [WiFi QR Code Format](#wifi-qr-code-format)
- [Architecture](#architecture)
- [File-by-File Breakdown](#file-by-file-breakdown)
- [Permissions Explained](#permissions-explained)
- [Database Schema](#database-schema)
- [QR Customisation System](#qr-customisation-system)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)

---

## What Is AirKey?

AirKey is a native Android app that solves a real daily problem: **you receive a WiFi QR code and have no idea what the password is**. Point AirKey at ANY WiFi QR code — from Android, iPhone, router admin panel, print-out, or screenshot — and it instantly decodes and shows you:

- The full network name (SSID)
- The password (tap to reveal)
- The security protocol (WPA2 / WPA3 / WEP / Open)
- Whether the network is hidden

Beyond scanning, AirKey is also a **designer-level QR code generator** with custom colours, dot shapes, gradients, frames, and center logos — all saved permanently in an offline local database.

---

## Features — Full List

### Scan Screen

| Feature | Detail |
|---|---|
| Live camera QR scanner | CameraX + ML Kit Barcode Scanning, 30 fps |
| Universal compatibility | Reads QR codes from Android, iPhone, routers, any app |
| SSID extraction | Full network name, handles special chars (`;`, `,`, `"`, `\`, `:`) |
| Password extraction | Fully decoded, including escaped characters |
| Security type detection | Identifies WPA/WPA2, WPA3, WEP, and Open (no password) |
| Hidden network flag | Shows `Yes — SSID not broadcast` or `No — Network is visible` |
| Security badge | Color-coded pill: WPA2=cyan, WPA3=green, WEP=orange, OPEN=red |
| Show / hide password | Toggle eye icon, defaults to hidden for privacy |
| Copy SSID | One-tap clipboard copy button next to SSID |
| Copy password | One-tap clipboard copy button next to password |
| Category assignment | Pick from Home / Work / Travel / Public / Guest / General before saving |
| Notes field | Add custom notes (e.g. "Coffee shop on Main St") before saving |
| Save to Vault | Persists to Room database, deduplicates by SSID |
| Generate custom QR | Pre-fills Generator screen with scanned network data |
| Open WiFi Settings | Direct Intent to Android WiFi settings for manual connection |
| Scan again | Reset scanner and scan another code |
| Haptic feedback | Double pulse vibration on successful scan (API 26+) |
| Animated scan overlay | Neon scanning line, color-alternating corner brackets, tip chips |
| Camera permission flow | Friendly permission screen with animated icon if not yet granted |

### Generator Screen (3 Tabs)

#### Details Tab

| Feature | Detail |
|---|---|
| SSID input | Network name field with WiFi icon |
| Password input | With show/hide toggle |
| Live password strength | Colour meter: Very Weak → Weak → Moderate → Good → Strong |
| Security type picker | WPA/WPA2, WPA3 (⭐ recommended), WEP (⚠️ legacy), Open |
| Security descriptions | Each type shows a one-line description below its label |
| Hidden network toggle | Switch with explanation text |
| Save to Vault | Button to permanently save the network |
| Pre-fill from scan | Generator auto-fills when launched from Scan screen |

#### Style Tab

| Feature | Detail |
|---|---|
| 8 colour themes | Neon Violet, Cyber Pink, Matrix Green, Solar Flame, Ocean Blue, Gold Rush, Midnight, Rose Gold |
| 4 colour patterns | Solid, Diagonal Gradient, Radial Gradient (centre-out), Horizontal Gradient |
| 5 dot shapes | Rounded Squares, Circles, Diamonds, 5-point Stars, Hearts |
| 4 frame styles | None, Glassmorphism border, Neon Glow (double ring with shadow), Cyberpunk corner brackets |
| Logo toggle | On/Off switch to show a center logo circle |
| Logo text input | Custom 1–3 character label (e.g. "AK", "WiFi", "🏠") — displayed in the QR center |
| Real-time preview | Every style change instantly regenerates the QR bitmap |

#### Preview Tab

| Feature | Detail |
|---|---|
| Full QR preview | 260dp card with animated radial glow behind it |
| Animated glow | Pulsing gradient halo using the selected foreground colour |
| Network label | Shows SSID name and "Scan to connect instantly" below QR |
| High error correction | H-level (30% data recovery) — QR still scans even with logo overlay |
| Compatibility row | Android Camera / iPhone Camera / Google Lens badges |
| Share QR | FileProvider intent — share PNG via WhatsApp, Email, Telegram, AirDrop, etc. |
| Save to Gallery | Saves PNG to `Pictures/AirKey/` on device |
| Empty state | Placeholder prompts user to fill Details tab first |

### Saved Networks Vault

| Feature | Detail |
|---|---|
| Persistent storage | Room (SQLite) — survives app restart, update, and device reboot |
| Expandable cards | Tap to expand/collapse details per network |
| Favorite toggle | Star icon → glowing gradient border on card |
| Favorites float to top | Starred networks shown first in list |
| Search | Real-time search across SSID and notes fields |
| Category filter | Horizontal chip row: All, Home, Work, Travel, Public, Guest, General |
| Category colour coding | Each category has a unique accent colour |
| Show / hide password | Per-card toggle, hidden by default |
| Copy password | Clipboard copy with one tap |
| Connect to WiFi | Uses `WifiNetworkSuggestion` API (Android 10+) to suggest connection |
| Generate QR | Opens Generator screen pre-filled with saved network data |
| Delete with confirm | Long-press or swipe triggers confirmation dialog before deleting |
| Last connected date | Shows when you last connected to this network |
| Saved date | Shows when the network was first saved |
| Notes display | Shows the notes field if set |
| Empty state animation | Animated floating icon with call-to-action when vault is empty |
| Animated cards | Card expand/collapse uses spring animation |

### Home Screen

| Feature | Detail |
|---|---|
| Animated background | 3 colour blobs (purple, cyan, pink) moving on infinite loop |
| Pulsing logo | AK logo ring scales on infinite transition |
| Network count stat | Live count of saved networks from DB |
| Quick action cards | Scan QR and Generate QR cards with gradient icons |
| Feature list | Shows core capabilities with icon+label rows |
| App version | Shown in footer |

### System / Global

| Feature | Detail |
|---|---|
| AMOLED dark theme | True black background (`#0A0A1A`), zero wasted battery on OLED |
| Neon colour palette | Purple `#6C63FF`, Cyan `#00F5FF`, Pink `#FF006E` |
| Toast system | Global snackbar-style feedback (saved, updated, deleted, error) |
| Edge-to-edge UI | Full bleed layout with `systemBarsPadding()` |
| Splash screen | Animated splash with AirKey logo using `core-splashscreen` |
| Bottom navigation | 4-tab nav bar (Home, Scan, Generate, Vault) with animated selection indicator |
| Navigation animations | Slide + fade transitions between screens |
| Compose BOM 2024 | Latest stable Compose UI components |
| Material 3 | Full MD3 theming, tokens, typography |
| Kotlin Coroutines | All DB operations off the main thread |
| StateFlow | Reactive UI updates via ViewModel → Compose |

---

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| Language | Kotlin | 1.9.22 |
| UI | Jetpack Compose + Material 3 | BOM 2024.02.02 |
| Navigation | Navigation Compose | 2.7.7 |
| Camera | CameraX (core, camera2, lifecycle, view) | 1.3.2 |
| QR Scanning | ML Kit Barcode Scanning | 17.2.0 |
| QR Generation | ZXing Core | 3.5.3 |
| Database | Room (runtime, ktx, KSP compiler) | 2.6.1 |
| State | ViewModel + StateFlow + Coroutines | 2.7.0 / 1.8.0 |
| Permissions | Accompanist Permissions | 0.34.0 |
| Splash | AndroidX SplashScreen | 1.0.1 |
| Build | Android Gradle Plugin | 8.2.2 |
| Gradle | Gradle Wrapper | 8.4 |
| Min SDK | Android 8.0 | API 26 |
| Target SDK | Android 14 | API 34 |

---

## Project Structure

```
AirKey/                                    ← Repository root
│
├── README.md                              ← This file (fully detailed)
│
├── .github/
│   └── workflows/
│       └── build-airkey-apk.yml          ← CI: auto-detects project location,
│                                            triggers on ANY push to main/master/develop
│
└── wifi-qr-app/                           ← Android project root
    │
    ├── .github/
    │   └── workflows/
    │       └── build-debug-apk.yml        ← CI: triggers when this subfolder IS the repo
    │
    ├── app/
    │   ├── build.gradle                   ← App module: SDK config, all dependencies
    │   ├── proguard-rules.pro
    │   └── src/main/
    │       ├── AndroidManifest.xml        ← Permissions, FileProvider, Activity
    │       ├── java/com/airkey/wifiqr/
    │       │   ├── MainActivity.kt        ← Entry point, navigation graph, bottom bar
    │       │   │
    │       │   ├── data/
    │       │   │   ├── WifiNetwork.kt     ← Room Entity + QR encoder + QR parser
    │       │   │   ├── WifiDatabase.kt    ← Room Database + DAO (all 10 queries)
    │       │   │   └── WifiRepository.kt  ← Repository pattern (data access layer)
    │       │   │
    │       │   ├── viewmodel/
    │       │   │   └── WifiViewModel.kt   ← All business logic, search, filter, connect
    │       │   │
    │       │   └── ui/
    │       │       ├── theme/
    │       │       │   ├── Theme.kt       ← AMOLED palette, colour tokens, MaterialTheme
    │       │       │   └── Typography.kt  ← Custom typography scale
    │       │       │
    │       │       ├── components/
    │       │       │   └── QrCodeGenerator.kt  ← Bitmap QR renderer: dots, frames, logo
    │       │       │
    │       │       └── screens/
    │       │           ├── HomeScreen.kt         ← Dashboard with animated background
    │       │           ├── ScanScreen.kt         ← Camera + ML Kit + result panel
    │       │           ├── GeneratorScreen.kt    ← 3-tab QR generator + style picker
    │       │           └── SavedNetworksScreen.kt ← Vault: search, filter, cards
    │       │
    │       └── res/
    │           ├── drawable/splash_icon.xml
    │           ├── mipmap-*/ic_launcher*.xml     ← Adaptive icon (all densities)
    │           ├── values/
    │           │   ├── colors.xml
    │           │   ├── strings.xml
    │           │   └── themes.xml                ← Splash screen theme
    │           └── xml/
    │               └── file_provider_paths.xml   ← FileProvider paths for QR sharing
    │
    ├── build.gradle                       ← Project-level: AGP + Kotlin + KSP plugins
    ├── settings.gradle                    ← Project name, module include
    ├── gradle.properties                  ← JVM args, AndroidX flag
    ├── gradlew / gradlew.bat              ← Gradle wrapper scripts
    └── README.md                          ← App-level README
```

---

## How to Build

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK: compile SDK 34, build tools 34.0.0
- Gradle 8.4 (wrapper included)

### Clone & Open

```bash
git clone https://github.com/<your-username>/AirKey.git
cd AirKey/wifi-qr-app
```

Open **`wifi-qr-app/`** as the project root in Android Studio.

### Build from Terminal

```bash
cd wifi-qr-app

# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease

# Install directly to connected device
./gradlew installDebug
```

Output:
```
wifi-qr-app/app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## GitHub Actions CI/CD

Two workflow files exist so CI works **regardless of how you push the repo**.

### Workflow 1 — Root level (recommended)
**File:** `.github/workflows/build-airkey-apk.yml`  
**Triggers:** Push to `main`, `master`, `develop` — and pull requests to `main`/`master`  
**Use case:** When you push the **whole workspace** (repo root contains `wifi-qr-app/` as a subfolder)

### Workflow 2 — Inside the Android project
**File:** `wifi-qr-app/.github/workflows/build-debug-apk.yml`  
**Triggers:** Same branches  
**Use case:** When `wifi-qr-app/` folder **is** the repository root (you pushed just that folder)

### What the workflow does (step by step)

```
Step 1 ── Checkout code (actions/checkout@v4)
Step 2 ── Auto-detect Android project location
          Checks: wifi-qr-app/settings.gradle → root/settings.gradle → find anywhere
Step 3 ── Install JDK 17 (Temurin)
Step 4 ── Install Android SDK (API 34, build-tools 34)
Step 5 ── Install Gradle 8.4 (gradle/actions/setup-gradle@v3)
Step 6 ── Download gradle-wrapper.jar at build time
          (binary not committed — downloaded from gradle/gradle GitHub)
Step 7 ── Restore Gradle cache (actions/cache@v4)
Step 8 ── Run: gradle assembleDebug --stacktrace --no-daemon
Step 9 ── Verify APK was produced
Step 10 ─ Upload APK as artifact (kept 30 days)
          Artifact name: AirKey-Debug-APK-{run_number}
Step 11 ─ Write Markdown summary to GitHub Actions job summary page
```

### Downloading the APK from GitHub Actions

1. Go to your repo on GitHub
2. Click **Actions** tab
3. Click the latest green ✅ workflow run
4. Scroll to **Artifacts** section at the bottom of the page
5. Download `AirKey-Debug-APK-{N}.zip`
6. Unzip → `app-debug.apk`
7. Transfer to phone → install (enable "Install from unknown sources" in settings first)

### Manual trigger

You can trigger a build without pushing code:
1. GitHub → Actions → `AirKey — Build Debug APK`
2. Click **Run workflow** → choose branch → **Run workflow**

---

## WiFi QR Code Format

AirKey uses the **standard `WIFI:` URI scheme**, identical to what Android, iPhone, and all major apps use. This means QR codes you generate with AirKey are scannable on any device, and QR codes from any other source are scannable by AirKey.

### Format Specification

```
WIFI:T:<security>;S:<ssid>;P:<password>;H:<hidden>;;
```

| Field | Key | Values | Example |
|---|---|---|---|
| Security type | `T` | `WPA`, `WEP`, `nopass` | `T:WPA` |
| Network name | `S` | Any UTF-8 string | `S:MyHomeWiFi` |
| Password | `P` | Any UTF-8 string | `P:supersecret123` |
| Hidden flag | `H` | `true` or `false` | `H:false` |

### Special Character Escaping

Characters `;` `,` `"` `\` `:` inside SSID or password are escaped with a backslash. AirKey handles this automatically — both when reading and writing QR codes.

```
; → \;     , → \,     " → \"     \ → \\     : → \:
```

### Real Examples

```
WIFI:T:WPA;S:HomeNetwork;P:password123;H:false;;
WIFI:T:WPA3;S:Office WiFi 5GHz;P:c0mpl3xPass!;H:false;;
WIFI:T:nopass;S:CoffeeShop_Guest;;;
WIFI:T:WEP;S:OldRouter;P:abcd1234;;
WIFI:T:WPA;S:Hidden Net;P:secret;H:true;;
```

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  HomeScreen  ScanScreen  GeneratorScreen  Vault  │
│              (Jetpack Compose)                   │
└──────────────────────┬──────────────────────────┘
                       │ collectAsState()
┌──────────────────────▼──────────────────────────┐
│                 ViewModel Layer                  │
│  WifiViewModel  ─  QrStyle  ─  UiState          │
│  (StateFlow + Coroutines)                        │
└──────────────────────┬──────────────────────────┘
                       │ suspend functions / Flow
┌──────────────────────▼──────────────────────────┐
│                Repository Layer                  │
│  WifiRepository  (wraps DAO calls)               │
└──────────────────────┬──────────────────────────┘
                       │ @Dao queries
┌──────────────────────▼──────────────────────────┐
│                  Data Layer                      │
│  WifiDatabase (Room/SQLite)  ←→  WifiNetwork     │
│  airkey_database.db                              │
└─────────────────────────────────────────────────┘

Camera:  CameraX → ImageAnalysis → ML Kit → parseWifiQrCode()
QR Gen:  ZXing BitMatrix → QrCodeGenerator.generate() → Bitmap
Share:   Bitmap → FileProvider → ACTION_SEND intent
```

**Pattern:** MVVM (Model-View-ViewModel) with Repository.  
**State:** Unidirectional data flow — UI reads from `StateFlow`, calls ViewModel functions, ViewModel updates state.  
**Threading:** All Room operations run on Coroutines IO dispatcher via `viewModelScope.launch`.

---

## File-by-File Breakdown

### `WifiNetwork.kt`
- **`SecurityType` enum** — 4 values: WPA, WPA3, WEP, OPEN. Each has a `display` string and a `wifiCode` for the QR format.
- **`WifiNetwork` data class** — Room Entity with 10 fields: `id`, `ssid`, `password`, `securityType`, `isHidden`, `notes`, `savedAt`, `lastConnected`, `category`, `isFavorite`.
- **`toWifiQrString()`** — Encodes a `WifiNetwork` into the standard `WIFI:T:...;;` format with full character escaping.
- **`ScannedWifiResult` data class** — Transient (not persisted), holds the decoded result from a QR scan.
- **`parseWifiQrCode(raw: String)`** — Regex-based parser: extracts S/P/T/H fields, unescapes special characters, returns null if not a WiFi QR code.

### `WifiDatabase.kt`
Room database with 10 DAO methods:

| Method | Type | Purpose |
|---|---|---|
| `getAllNetworks()` | `Flow<List>` | All networks, newest first |
| `getFavoriteNetworks()` | `Flow<List>` | Starred only |
| `searchNetworks(query)` | `Flow<List>` | SSID OR notes LIKE search |
| `getByCategory(category)` | `Flow<List>` | Filter by category |
| `getById(id)` | `suspend` | Single network lookup |
| `getBySsid(ssid)` | `suspend` | Deduplication check |
| `insert(network)` | `suspend` | Add / replace |
| `update(network)` | `suspend` | Edit existing |
| `delete(network)` | `suspend` | Remove |
| `setFavorite(id, isFav)` | `suspend` | Toggle star |
| `updateLastConnected(id)` | `suspend` | Update timestamp on connect |

### `WifiViewModel.kt`
- Holds `UiState` (networks list, loading, search query, category, toast message, count).
- Holds `QrStyle` (colour theme, pattern, dot shape, frame, logo toggle, logo text).
- Combines `_searchQuery` + `_selectedCategory` flows into a `flatMapLatest` to efficiently switch Room queries.
- `onQrScanned(raw)` → calls `parseWifiQrCode` → publishes to `_scannedResult`.
- `saveNetwork()` → deduplicates by SSID via `getBySsid()` → inserts or updates.
- `connectToWifi()` → uses `WifiNetworkSuggestion` API on Android 10+; older devices open settings.
- `toggleFavorite()` → direct DAO call to flip `isFavorite`.

### `QrCodeGenerator.kt`
Pure-Kotlin `object` that renders a styled QR code to a `Bitmap` using ZXing + Android Canvas.

**Pipeline:**
1. ZXing encodes content to `BitMatrix` at H-level error correction (30% recovery)
2. `drawBackground()` — applies solid, glass, neon, or cyberpunk background
3. Loop over all `1` cells in the matrix → `drawDot()` with colour interpolation
4. `drawFrame()` — overlays border decoration (none / glass / neon glow / corner brackets)
5. `drawLogoCenter()` — draws a gradient circle with text at the QR centre

**Colour patterns:**
- `0` Solid — flat foreground colour
- `1` Diagonal — linear interpolation across x+y axes
- `2` Radial — interpolation from centre outward
- `3` Horizontal — gradient left-to-right

**Dot shapes (non-finder cells):**
- `0` Rounded square (50% corner radius)
- `1` Circle
- `2` Diamond (rotated square path)
- `3` Star (5-point parametric path)
- `4` Heart (cubic Bezier path)

### `ScanScreen.kt`
- `CameraPreviewSection` — binds `Preview` + `ImageAnalysis` to lifecycle; ML Kit processes frames, throttled to 800ms.
- `ScanOverlay` — draws corner brackets (Canvas custom drawing), animated scan line (gradient shader), tip chips.
- `ScannedResultPanel` — shows all decoded fields with `DetailRow` components, category chip picker, notes input, action buttons.
- `PermissionScreen` — shown when camera permission not granted; animated pulsing icon.
- `vibrate()` — dual-pulse haptic using `VibratorManager` (API 31+) or legacy `Vibrator`.

### `GeneratorScreen.kt`
- 3-tab layout using custom animated tab row with gradient background on selected tab.
- `DetailsTab` — form fields + security type radio group + hidden toggle + save button.
- `StyleTab` — horizontal scrollable rows for themes, patterns, dot shapes, frames, logo config.
- `PreviewTab` — live QR bitmap display with glow animation + share + save-to-gallery.
- `airKeyTextFieldColors()` — top-level function shared with `SavedNetworksScreen.kt` (same package, no import needed).
- `passwordStrength()` — checks length, uppercase, lowercase, digits, special chars → returns colour + label.
- `shareQrCode()` — writes bitmap to cache dir, gets FileProvider URI, fires `ACTION_SEND` intent.
- `saveQrToGallery()` — uses `MediaStore` API (Android 10+) or legacy `Environment` path.

### `SavedNetworksScreen.kt`
- `LazyColumn` of expandable `NetworkCard` composables.
- Each card shows: SSID, category badge, favorite star, saved date.
- Expanded state shows: password (hidden/shown), notes, last connected, category, action buttons.
- Delete requires a confirmation `AlertDialog`.
- `connectToWifi()` calls ViewModel which uses `WifiNetworkSuggestion`.
- `GenerateQr` button navigates to Generator screen pre-filled with the saved network.

### `MainActivity.kt`
- `installSplashScreen()` → shows splash on cold start.
- `enableEdgeToEdge()` → full bleed behind system bars.
- `NavHost` with 4 destinations: `home`, `scan`, `generate?ssid=...`, `saved`.
- Generate route accepts URL-encoded query params to pre-fill from scan or vault.
- Custom animated bottom navigation bar with gradient selected-state indicator and scale animation.
- `LaunchedEffect(uiState.toastMessage)` → shows toast + auto-clears after 2 seconds.

---

## Permissions Explained

| Permission | Why |
|---|---|
| `CAMERA` | Required to scan QR codes with CameraX + ML Kit |
| `ACCESS_WIFI_STATE` | Read current WiFi connection info |
| `CHANGE_WIFI_STATE` | Used by WifiNetworkSuggestion to suggest connection |
| `ACCESS_NETWORK_STATE` | Check network connectivity status |
| `ACCESS_FINE_LOCATION` | Required by Android for WiFi scanning (OS mandate) |
| `ACCESS_COARSE_LOCATION` | Same — Android requires location for WiFi access |
| `VIBRATE` | Haptic feedback on successful QR scan |
| `WRITE_EXTERNAL_STORAGE` | Save QR images on Android 8/9 (maxSdkVersion 28) |
| `READ_EXTERNAL_STORAGE` | Read media on Android 12 and below (maxSdkVersion 32) |
| `INTERNET` | Not actively used; included as safe default |

> **Note:** Location permissions are required by Android OS to access WiFi APIs — AirKey never reads or stores your physical location.

---

## Database Schema

**Table:** `wifi_networks`

| Column | Type | Description |
|---|---|---|
| `id` | `INTEGER` PRIMARY KEY AUTOINCREMENT | Unique row identifier |
| `ssid` | `TEXT` NOT NULL | WiFi network name |
| `password` | `TEXT` NOT NULL | Network password (plain text, local only) |
| `securityType` | `TEXT` NOT NULL DEFAULT `'WPA'` | Enum name: WPA, WPA3, WEP, OPEN |
| `isHidden` | `INTEGER` NOT NULL DEFAULT `0` | `0` = visible, `1` = hidden SSID |
| `notes` | `TEXT` NOT NULL DEFAULT `''` | User notes (free text) |
| `savedAt` | `INTEGER` NOT NULL | Unix timestamp ms — when first saved |
| `lastConnected` | `INTEGER` | Unix timestamp ms — last successful connect (nullable) |
| `category` | `TEXT` NOT NULL DEFAULT `'General'` | One of: Home, Work, Travel, Public, Guest, General |
| `isFavorite` | `INTEGER` NOT NULL DEFAULT `0` | `0` = normal, `1` = starred |

**Database file:** `airkey_database` (Room SQLite, version 1)  
**Migration strategy:** `fallbackToDestructiveMigration()` (clears and recreates on schema change during development)

---

## QR Customisation System

The `QrStyle` data class drives everything:

```kotlin
data class QrStyle(
    val patternIndex: Int = 0,        // 0=Solid, 1=Diagonal, 2=Radial, 3=Horizontal
    val foregroundColor: Long,        // Primary QR dot colour (ARGB)
    val backgroundColor: Long,        // QR background colour
    val accentColor: Long,            // Secondary gradient colour
    val dotShape: Int = 0,            // 0=Rounded, 1=Circle, 2=Diamond, 3=Star, 4=Heart
    val frameStyle: Int = 0,          // 0=None, 1=Glass, 2=Neon Glow, 3=Cyberpunk
    val showLogo: Boolean = false,    // Show center logo circle
    val logoText: String = "AK"       // Text inside the logo (max 3 chars)
)
```

**8 Built-in Themes:**

| Theme | Foreground | Accent |
|---|---|---|
| Neon Violet | `#6C63FF` | `#00F5FF` |
| Cyber Pink | `#FF006E` | `#6C63FF` |
| Matrix Green | `#00E676` | `#00BCD4` |
| Solar Flame | `#FFAB40` | `#FF5252` |
| Ocean Blue | `#0288D1` | `#00E5FF` |
| Gold Rush | `#FFD700` | `#FF8C00` |
| Midnight | `#7B1FA2` | `#3F51B5` |
| Rose Gold | `#FF6090` | `#FFB347` |

**Error correction level:** H (High) — 30% of the QR data can be obscured/damaged and it still scans. This is essential when using a center logo.

---

## Troubleshooting

### QR code not scanning
- Ensure the camera is within 15–60 cm of the QR code
- Clean the camera lens
- The QR code must start with `WIFI:` — plain text or URLs are ignored
- Try better lighting or reduce glare

### "Camera permission denied" won't go away
- Go to phone Settings → Apps → AirKey → Permissions → Camera → Allow
- Restart the app

### WiFi connection suggestion not working
- This feature requires Android 10 (API 29) or higher
- On older devices, tap "Open WiFi Settings" to connect manually
- The system may show a notification to confirm the network suggestion

### Build fails on GitHub Actions
- Check that `gradlew` has execute permissions: `git update-index --chmod=+x wifi-qr-app/gradlew`
- Verify the correct project path is detected in the workflow auto-detect step

### APK won't install on phone
- Enable "Install from unknown sources": Settings → Security → Install unknown apps → allow your file manager
- The debug APK uses `applicationId "com.airkey.wifiqr.debug"` — it coexists with a release version

---

## Roadmap

- [ ] Biometric lock for the password vault
- [ ] WiFi network export (encrypted `.wifiwallet` backup)
- [ ] Import from backup file
- [ ] Widget for quick scan on home screen
- [ ] QR code label printing (PDF export with custom layout)
- [ ] Dark/light theme toggle (currently always dark)
- [ ] Search history
- [ ] Network signal strength display
- [ ] Multiple QR export (all saved networks as a PDF booklet)

---

## License

```
MIT License

Copyright (c) 2024 AirKey

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

---

<div align="center">

Built with ❤️ using Kotlin + Jetpack Compose

**AirKey** — Every WiFi secret, decoded.

</div>
