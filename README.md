# AirKey — WiFi QR Code Manager

> **The most advanced WiFi QR platform. Scan any QR code → get all WiFi details instantly.**

![Build Debug APK](../../workflows/Build%20Debug%20APK/badge.svg)

---

## What It Does

Most phones either show *only* a QR code to share WiFi, or *only* show the password — but not both. **AirKey** fixes this.

**Scan any WiFi QR code from any phone/app → instantly see:**
- WiFi Name (SSID)
- Password (in plain text, with copy button)
- Security type (WPA/WPA2/WPA3/WEP/Open)
- Hidden network status

Then generate a **stunning custom QR code** with that info so others can connect in one scan.

---

## Features

### Scan
- Camera-based QR scanner with animated neon overlay
- ML Kit barcode scanning (works with any standard WiFi QR)
- Instant decode with haptic feedback
- One-tap copy for SSID and password
- Direct flow to generate custom QR from scanned data

### Generate Custom QR Codes
- **Color themes** — 6 presets: Neon Violet, Cyber Pink, Matrix, Solar, Ocean, Gold
- **Color patterns** — Solid, Diagonal Gradient, Radial Gradient, Horizontal
- **Dot shapes** — Rounded, Circle, Diamond, Star, Heart
- **Frame styles** — None, Glassmorphism, Neon Glow, Cyberpunk corners
- **Center logo** — Enable with custom text (up to 2 chars)
- **High error correction** (H level) — QR still scans even with logo overlay
- **Share** — Share QR image via any app
- **Save to gallery** — PNG saved to Pictures/AirKey

### Network Vault (Permanent Storage)
- Room database — survives app restarts, uninstall-safe with device backup
- Search by name or notes
- Category filter (All, Home, Work, Travel, Public, Guest, General)
- Favorite networks with star/heart toggle
- Expandable cards with show/hide password
- Copy password to clipboard
- Connect button (adds WiFi suggestion on Android 10+)
- Delete with confirmation

### UI
- AMOLED dark theme with neon purple + cyan palette
- Animated blob background on home screen
- Animated scan line overlay in scanner
- Floating glassmorphism bottom nav bar
- Smooth slide + fade screen transitions
- Toast notifications
- Edge-to-edge with proper insets

---

## Tech Stack

| Layer | Tech |
|-------|------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Database | Room + KSP |
| Camera | CameraX |
| QR Scan | ML Kit Barcode Scanning |
| QR Generate | ZXing Core |
| Architecture | ViewModel + StateFlow + Repository |
| Build | Gradle 8.4 + AGP 8.2.2 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Build & Install

### GitHub Actions (Automatic)
Every push to `main`/`master` triggers a debug APK build. Download from the **Actions** tab → latest workflow run → **AirKey-Debug-APK** artifact.

### Local Build
```bash
# Prerequisites: Android Studio / Android SDK, JDK 17
git clone <this-repo>
cd wifi-qr-app
./gradlew assembleDebug

# APK output:
# app/build/outputs/apk/debug/app-debug.apk
```

Install on device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| CAMERA | QR code scanning |
| ACCESS_WIFI_STATE | Read current WiFi info |
| CHANGE_WIFI_STATE | Connect to networks (Android 10+) |
| ACCESS_FINE_LOCATION | Required by Android for WiFi operations |
| VIBRATE | Haptic feedback on successful scan |
| WRITE_EXTERNAL_STORAGE | Save QR images (Android 9 and below) |

---

## QR Code Format

AirKey reads and generates standard WiFi QR codes in the universal format:
```
WIFI:T:WPA;S:NetworkName;P:Password;H:false;;
```
This format is readable by:
- Android's built-in camera
- iOS camera
- Google Lens
- Any standard QR scanner app

---

## Project Structure

```
wifi-qr-app/
├── .github/workflows/build-debug-apk.yml   # CI/CD — builds debug APK
├── app/src/main/
│   ├── java/com/airkey/wifiqr/
│   │   ├── MainActivity.kt                  # App entry, nav graph, bottom bar
│   │   ├── data/
│   │   │   ├── WifiNetwork.kt               # Entity + QR parser
│   │   │   ├── WifiDatabase.kt              # Room DB + DAO
│   │   │   └── WifiRepository.kt            # Data layer
│   │   ├── viewmodel/
│   │   │   └── WifiViewModel.kt             # State + business logic
│   │   └── ui/
│   │       ├── theme/                       # Colors, typography, theme
│   │       ├── components/
│   │       │   └── QrCodeGenerator.kt       # Custom QR bitmap renderer
│   │       └── screens/
│   │           ├── HomeScreen.kt            # Dashboard
│   │           ├── ScanScreen.kt            # Camera scanner + result
│   │           ├── GeneratorScreen.kt       # QR generator + styler
│   │           └── SavedNetworksScreen.kt   # Vault with search/filter
│   ├── res/                                 # Drawables, icons, themes
│   └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```
