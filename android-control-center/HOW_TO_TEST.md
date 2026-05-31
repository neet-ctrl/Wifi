# How to Test Every Feature — Android Control Center Ultimate

> This guide tells you **exactly what to tap, what to expect, and what the real result is** for every feature. All features run through `AccuConnectionManager` — one connection, all 85+ screens.

---

## Setup: Connect ACCU First

**Before testing ANY feature, establish a privilege connection. Pick one method:**

### Option A — Root (easiest, if rooted)
```
ACCU → ACCU Center → tap "Use Root"
→ Status card turns green: "ACCU Connected · Root · uid=0"
```

### Option B — Wireless ADB (Android 11+, no root needed)

**On the TARGET phone (phone you want to control):**
1. `Settings → About phone → Build number` — tap 7 times → Developer mode ON
2. `Settings → Developer Options → Wireless debugging → toggle ON`
3. `Settings → Developer Options → Wireless debugging → Pair device with pairing code`
4. Note the **6-digit code** and **pairing port** shown (e.g. code: `123456`, port: `37839`)

**On the HOST phone (running ACCU):**
1. `ACCU → ACCU Center → tap "Wireless ADB"`
2. ACCU auto-discovers the target via mDNS — a notification appears when found
3. In ACCU Center, enter the 6-digit code → tap Confirm
4. Status card: `"ACCU Connected · Wireless ADB (192.168.x.x)"`

> Both phones must be on the same Wi-Fi network (or one is hosting a hotspot the other joins).

### Option C — OTG / USB ADB (cable, no Wi-Fi needed)

**On the TARGET phone:**
1. `Settings → Developer Options → USB debugging → ON`

**Connect the phones:**
```
HOST phone ←[USB-OTG adapter]←[cable]→ TARGET phone
```

**In ACCU on the HOST phone:**
1. `ACCU Center → tap "OTG / USB"`
2. Approve "Allow USB debugging?" dialog on the TARGET phone
3. Status card: `"ACCU Connected · OTG / USB ADB"`

---

## How the connection serves all features

Once connected via **any** method above, the status card shows green. Every screen in the app automatically uses that connection — Shell, App Manager, Freeze, QS Tiles, File Manager, everything. You never need to re-connect when switching features.

---

## Quick Test Table

| Feature | Where | What happens | Real result |
|---------|-------|-------------|-------------|
| Shell terminal | Shell tab | Type `pm list packages` | Lists ALL installed packages live |
| Freeze an app | App Manager → Freeze | Tap freeze on any app | `pm suspend --user 0 <pkg>` — app can't launch |
| Grant permission | App Explorer → any app → Permissions | Toggle dangerous permission | `pm grant <pkg> <perm>` — switch turns green |
| Debloat | App Manager → Debloat | Pick system app, tap Remove | `pm uninstall --user 0 <pkg>` runs |
| Dark mode per app | Customization → Per-App Dark Mode | Toggle switch | Overlay via WRITE_SECURE_SETTINGS |
| QS Tiles | Network Center | Tap "Add Wi-Fi Tile" | Tile added to Quick Settings panel |
| File chmod | File Manager → long-press file → Properties → Permissions | Toggle and Apply | `chmod` via AccuConnectionManager |

---

## 1. Shell Terminal

**Where:** Shell tab (bottom navigation)

### 1.1 — Basic command execution
1. Tap the Shell tab
2. Type: `pm list packages -3`
3. Tap Send
4. **Expected:** Live scrollable list of all user package names

### 1.2 — Verify connection method in shell
1. Type: `id`
2. **Expected:** `uid=0(root)` if root, `uid=2000(shell)` if wireless/OTG ADB

### 1.3 — Command history
1. Run any command, tap ↑ arrow
2. **Expected:** Previous command fills input

### 1.4 — Autocomplete
1. Type `pm ` (with space)
2. **Expected:** Suggestion chips (grant, revoke, list, etc.)

---

## 2. Debloat / Canta

**Where:** App Manager tab → "Debloat" chip

### 2.1 — Remove a safe bloatware app
1. Find app marked Safe
2. Tap → "Remove for user"
3. **Expected:** `pm uninstall --user 0 <pkg>` runs

### 2.2 — Restore a removed app
1. "Removed" filter chip → find app → "Restore"
2. **Expected:** `pm install-existing <pkg>` runs

---

## 3. Freeze Apps (Hail)

**Where:** App Manager tab → "Freeze" chip

### 3.1 — Freeze an app
1. Tap Freeze button on any app
2. **Expected:** `pm suspend --user 0 <pkg>` — app shows ❄ badge, unlaunchable

### 3.2 — Unfreeze
1. Tap frozen app → "Unfreeze"
2. **Expected:** `pm unsuspend --user 0 <pkg>`

### 3.3 — Freeze All QS tile
1. Add "Freeze All" tile to Quick Settings
2. Tap it
3. **Expected:** All apps in freeze list suspended instantly

---

## 4. Permission Manager (Inure/Blocker)

**Where:** App Manager tab → "Permissions" chip

### 4.1 — Revoke a dangerous permission
1. Find app with location permission → tap → toggle OFF
2. **Expected:** `pm revoke <pkg> android.permission.ACCESS_FINE_LOCATION`

### 4.2 — Grant a permission
1. Toggle revoked permission ON
2. **Expected:** `pm grant <pkg> <permission>` — switch turns green

---

## 5. App Explorer

**Where:** App Manager tab → "Explorer" chip

### 5.1 — Run a shell command per app
1. Expand any app → tap "Shell Commands" → tap ▶ on "Memory Stats"
2. **Expected:** `dumpsys meminfo <pkg>` output shown

### 5.2 — Toggle dangerous permission inline
1. Expand any app → Permissions → toggle switch
2. **Expected:** Instant toggle with `pm grant/revoke`

---

## 6. Component Manager (Blocker)

**Where:** App Manager → Components

### 6.1 — Block a tracker component
1. Find app → find analytics component → "Block via IFW"
2. **Expected:** Intent Firewall rule applied via ADB shell

---

## 7. Audio DSP (RootlessJamesDSP)

**Where:** Audio Center tab

### 7.1 — Enable DSP (does NOT need ADB connection)
1. Toggle "Enable Audio DSP" ON → play audio
2. **Expected:** Audio passes through JamesDSP engine

### 7.2 — Parametric EQ
1. Tap "Parametric EQ" → drag 100Hz up +6dB
2. **Expected:** Bass noticeably louder

---

## 8. Call Recording

**Where:** Call Recorder tab

### 8.1 — Enable recording
1. Toggle "Enable Call Recording" ON
2. Make a call → hang up
3. **Expected:** Recording appears in list

---

## 9. Internet Tiles

**Where:** Network Center tab → Tiles section

### 9.1 — Toggle Wi-Fi from QS without popup
1. Add Wi-Fi tile → tap it
2. **Expected:** Wi-Fi toggles directly via `svc wifi enable/disable`

### 9.2 — Mobile Data tile
1. Add Mobile Data tile → tap it
2. **Expected:** `svc data enable/disable` via AccuConnectionManager

---

## 10. Key Mapper

**Where:** Automation tab → Key Mapper

### 10.1 — Remap volume down to screenshot
1. New mapping → trigger: Volume Down double-tap → action: Take Screenshot
2. **Expected:** Double-tapping volume down takes screenshot

---

## 11. Language Selector

**Where:** Language Center tab

### 11.1 — Change app language
1. Find any app → select "Français"
2. **Expected:** `am broadcast --user 0 -a android.intent.action.LOCALE_CHANGED`

---

## 12. File Manager

**Where:** File Manager tab

### 12.1 — Change file permissions
1. Long-press file → Properties → Permissions tab
2. Toggle execute, tap "Apply via ACCU"
3. **Expected:** `chmod` via AccuConnectionManager.exec() — success or clear error shown

### 12.2 — FTP server
1. Advanced → Start FTP Server
2. **Expected:** FTP URL shown — accessible from any FTP client on LAN

---

## 13. Customization / Theme Engine

**Where:** Customization tab

### 13.1 — Apply theme preset
1. Tap "Neon Matrix" → Apply Theme
2. **Expected:** Entire app switches to green-on-black immediately

---

## 14. Storage Cleaning (SD Maid SE)

**Where:** Storage tab

### 14.1 — Clean app cache
1. App Cleaner → Scan → select apps → Clean Selected
2. **Expected:** `pm clear --cache-only <pkg>` for each

---

## 15. ACCU Center — Connection Diagnostics

**Where:** ACCU Center → overflow menu → Run Diagnostics

1. Tap Diagnostics icon in the top bar
2. **Expected:** Log output showing:
   - Android version, SDK, device model
   - Current UID (0 = root, 2000 = ADB shell)
   - ADB enabled status
   - Wireless ADB status
   - Connection method: Root / Wireless ADB / OTG

### Verify connection flows from ACCU Center
```
Tap "Wireless ADB" button   → starts mDNS pairing discovery
Tap "OTG / USB" button      → runs adb devices to detect USB-connected phone
Tap "Use Root" button       → tests su -c id, confirms root available
Tap "Restart" button        → re-runs adb connect <last_ip>:<last_port>
Tap "Stop" button           → disconnects, saves nothing
```

---

## How to confirm features are real (not placeholder)

| Proof | How to check |
|-------|-------------|
| Commands execute | Run `pm list packages` in Shell — real packages appear |
| Permissions toggle | Before/after: check Settings → Apps → Permissions |
| Apps freeze | After freeze: try to open the app — it won't launch |
| Theme changes | Pick "Neon Matrix" — app goes green immediately |
| Cache cleared | Check Storage in Settings before/after "Clean Cache" |
| Connection method | ACCU Center status card + `id` command in shell |

---

## Known Requirements per Feature

| Feature group | Requirement |
|---------------|-------------|
| All privileged features | Root OR Wireless ADB OR OTG ADB (one of the three) |
| Wireless ADB auto-pair | Android 11+ on target (API 30+), same Wi-Fi network |
| OTG ADB | Host phone: USB OTG host mode support; target: USB debugging ON |
| Root features | Rooted device with LibSU access |
| Audio DSP | Works without privilege (Accessibility Service) |
| Call recording | Works on most devices; MIUI/OneUI may need extra setup |
| QS tiles | Android 10+ for programmatic tile add |
| Per-app dark mode | Android 10+ |
| Device admin features | User must manually enable ACCU as Device Administrator |
