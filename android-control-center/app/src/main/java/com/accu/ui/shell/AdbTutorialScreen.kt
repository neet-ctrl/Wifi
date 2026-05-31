package com.accu.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accu.ui.components.ACCTopBar
import com.accu.ui.theme.AccentCyan
import com.accu.ui.theme.AccentGreen
import com.accu.ui.theme.AccentOrange
import com.accu.ui.theme.AccentRed

// ── Data model ────────────────────────────────────────────────────────────────

private data class TutorialStep(
    val title: String,
    val body: String,
    val code: String? = null,
    val tip: String? = null,
    val warning: String? = null,
)

private data class TutorialSection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val steps: List<TutorialStep>,
)

// ─────────────────────────────────────────────────────────────────────────────

private val WIFI_ADB_TUTORIAL = TutorialSection(
    title    = "Wi-Fi ADB",
    subtitle = "Connect wirelessly — ACCU auto-pairs, no IP typing needed",
    icon     = Icons.Default.Wifi,
    color    = AccentCyan,
    steps    = listOf(
        TutorialStep(
            title = "Step 1 — Unlock Developer Options on the TARGET phone",
            body  = "The phone you want to control must have Developer Options unlocked:",
            code  = """Settings  →  About phone  →  Build number
Tap \"Build number\" 7 times rapidly
→ \"You are now a developer!\"

Then go to:
Settings  →  Developer Options""",
            tip   = "Samsung: Settings → About phone → Software information → Build number. MIUI/HyperOS: Settings → About phone → tap MIUI version 7×.",
        ),
        TutorialStep(
            title   = "Step 2 — Enable Wireless Debugging on the TARGET phone",
            body    = "In Developer Options on the TARGET device:",
            code    = "Settings → Developer Options → Wireless debugging → Toggle ON",
            tip     = "Both phones must be on the same Wi-Fi network (same router/AP). Using one phone as a hotspot that the other joins also works.",
            warning = "Wireless debugging turns off automatically when you disconnect from Wi-Fi or restart the device.",
        ),
        TutorialStep(
            title = "Step 3 — Get the 6-digit pairing code from the TARGET phone",
            body  = "Still on the TARGET phone:",
            code  = """Settings → Developer Options → Wireless debugging
→ tap \"Pair device with pairing code\"

You will see:
  Wi-Fi pairing code:  123456
  IP address & Port:   192.168.1.42:37839
                                   ↑ this is the PAIRING port

On the main Wireless debugging page you also see:
  IP address & Port:   192.168.1.42:41547
                                   ↑ this is the SESSION/CONNECTION port

ACCU finds both ports automatically — you only need the 6-digit code.""",
            tip   = "The pairing code expires after ~60 seconds. Have ACCU ready on the host phone before requesting the code.",
        ),
        TutorialStep(
            title = "Step 4 — Tap \"Wireless ADB\" in ACCU on the HOST phone",
            body  = "On the HOST phone (the one running ACCU):",
            code  = """ACCU  →  bottom nav: ACCU Center (shield icon)
→ Status card shows: \"Not Connected\"
→ Tap the \"Wireless ADB\" button

ACCU starts mDNS auto-discovery of Wireless Debugging services.
When your target is found:
• A notification appears: \"Wireless Debugging Detected — Open ACCU\"
• OR: ACCU Center advances to the pairing code step automatically""",
            tip   = "If discovery takes more than 30 seconds, check both phones are on the same Wi-Fi network and that the target's Wireless debugging is still ON.",
        ),
        TutorialStep(
            title = "Step 5 — Enter the 6-digit code in ACCU",
            body  = "In ACCU on the HOST phone:",
            code  = """ACCU Center → Pairing step
→ Enter the 6-digit code shown on the TARGET phone
→ Tap \"Confirm\"

ACCU then runs automatically:
  adb pair 192.168.1.42:37839 123456   ← pairing
  adb connect 192.168.1.42:41547       ← connect to session port

Status card turns green:
  \"ACCU Connected · Wireless ADB (192.168.1.42) · uid=2000\"""",
            tip   = "Pairing is a one-time step per Wi-Fi network. After first pair, ACCU only needs to run adb connect on future sessions.",
        ),
        TutorialStep(
            title = "All features now work!",
            body  = "Every screen in ACCU automatically routes through the wireless ADB session. You never need to re-connect when switching features.",
            code  = """Shell terminal     → uid=2000 ADB shell
App Freeze         → pm suspend --user 0 <pkg>
Permission toggle  → pm grant/revoke <pkg> <perm>
Debloat            → pm uninstall --user 0 <pkg>
QS Tiles           → svc wifi enable/disable
File chmod         → chmod via AccuConnectionManager
Diagnostics        → id  →  shows uid=2000(shell)""",
            tip   = "To reconnect after device reboot: ACCU Center → tap \"Restart\". Full re-pairing is not needed — just reconnect.",
        ),
        TutorialStep(
            title = "Reconnect after reboot",
            body  = "After the target phone restarts, Wireless debugging may be off. Re-enable it, then:",
            code  = """ACCU Center → tap \"Restart\"
→ ACCU runs: adb connect <last_saved_IP>:<last_saved_port>
→ Reconnects in ~1 second if Wireless debugging is still ON

If it fails (e.g. port changed):
→ Tap \"Wireless ADB\" to re-discover via mDNS
→ Re-enter the 6-digit code (no need to re-enable Developer Options)""",
        ),
        TutorialStep(
            title = "Legacy: adb tcpip method (Android 10 and below)",
            body  = "For older phones without the Wireless Debugging menu. Requires USB + OTG cable first:",
            code  = """1. Connect phones via OTG cable
2. adb devices  →  confirm target is listed
3. adb tcpip 5555
4. Disconnect USB cable
5. adb connect <TARGET_IP>:5555
6. Verify: adb devices  →  <IP>:5555  device""",
            warning = "Android 11+ does not need this method — use the guided Wireless ADB flow in ACCU Center instead.",
        ),
        TutorialStep(
            title = "Disconnect",
            body  = "When done:",
            code  = """ACCU Center → tap \"Stop\"
→ ACCU runs: adb disconnect <ip>:<port>
→ Clears saved session

Or from shell:
  adb disconnect 192.168.1.42:41547""",
            tip   = "Toggle off Wireless Debugging on the target to prevent any further connection attempts.",
        ),
    )
)

private val OTG_ADB_TUTORIAL = TutorialSection(
    title    = "OTG ADB (USB)",
    subtitle = "Control one Android device from another via USB cable — no Wi-Fi needed",
    icon     = Icons.Default.Usb,
    color    = AccentOrange,
    steps    = listOf(
        TutorialStep(
            title   = "Step 1 — Check hardware requirements",
            body    = "You need specific hardware for phone-to-phone USB ADB:",
            code    = """HOST phone (running ACCU):
  • Must support USB OTG HOST mode
  • Android 5.0+ recommended
  • Most phones from 2018+ support it (Pixel, Samsung, OnePlus, Xiaomi)

TARGET phone (being controlled):
  • Any Android phone
  • USB debugging enabled (we'll set this in Step 2)

Cable — pick one option:
  A) USB-C OTG adapter on HOST  +  USB-C cable to TARGET
  B) USB-A OTG adapter on HOST  +  USB-A-to-C cable to TARGET
  C) USB-C to USB-C OTG cable   (check label for OTG/host support)""",
            tip     = "Not sure if your HOST phone supports USB OTG? Try connecting a USB mouse — if the cursor appears, OTG host mode is supported.",
            warning = "A standard phone charging cable alone will NOT work. You need a cable or adapter with USB OTG host capability.",
        ),
        TutorialStep(
            title = "Step 2 — Enable USB Debugging on the TARGET phone",
            body  = "On the TARGET phone (the phone you want to control):",
            code  = """1. Settings  →  About phone  →  Build number
   Tap 7 times rapidly  →  \"You are now a developer!\"

2. Settings  →  Developer Options
   →  USB debugging  →  Toggle ON

3. Recommended extras:
   →  Install via USB  →  ON
   →  USB debugging (Security settings)  →  ON""",
            tip   = "On Samsung: USB debugging is under Settings → Developer Options → USB debugging. On MIUI: also enable \"USB Debugging (Security Settings)\" for full pm/am access.",
        ),
        TutorialStep(
            title = "Step 3 — Connect the two phones with your OTG cable",
            body  = "Connect the phones physically:",
            code  = """HOST phone  ←[OTG adapter/cable]→  TARGET phone

Option A (most common):
  HOST USB-C port  →  USB-C OTG adapter  →  USB-C cable  →  TARGET USB-C port

Option B (older HOST phone):
  HOST Micro-USB  →  Micro-USB OTG adapter  →  USB-A cable  →  TARGET

Option C (direct cable):
  USB-C to USB-C OTG cable (one end must be marked HOST/OTG)""",
            tip   = "If using a USB-A OTG adapter, plug the OTG adapter into the HOST phone, then connect the USB-A cable to the TARGET phone's USB-C port using a USB-A to C cable.",
            warning = "Some USB-C cables are \"charge only\" — they won't carry data. Use a cable that supports USB 2.0 or higher data transfer.",
        ),
        TutorialStep(
            title = "Step 4 — Tap \"OTG / USB\" in ACCU on the HOST phone",
            body  = "On the HOST phone (the one running ACCU):",
            code  = """ACCU  →  bottom nav: ACCU Center (shield icon)
→ Status card shows: \"Not Connected\"
→ Tap the \"OTG / USB\" button

ACCU runs: adb devices  (looks for USB-connected device)

If a device is found:
  Status card: \"ACCU Connected · OTG / USB ADB · uid=2000\"

If \"no devices found\":
  → Unlock the TARGET phone screen and check for a dialog""",
            warning = "If connection fails: unlock the target screen, swap the cable direction, or tap \"Change USB preference\" in the target's notification shade.",
        ),
        TutorialStep(
            title = "Step 5 — Approve USB Debugging on the TARGET phone",
            body  = "The TARGET phone will show a one-time approval dialog:",
            code  = """[TARGET phone shows]
\"Allow USB debugging?\"
RSA key fingerprint: AB:CD:EF:...

☑ Always allow from this computer   ← tick this
→ Tap [Allow]

If the dialog doesn't appear:
  • Swipe down the notification shade on the TARGET
  • Look for \"USB preference\" notification → change to \"File Transfer\"
  • Then go back to ACCU Center and tap \"OTG / USB\" again""",
            tip   = "Tick \"Always allow\" so you won't need to approve every time you reconnect with this cable.",
        ),
        TutorialStep(
            title = "Step 6 — All features now run on the TARGET phone",
            body  = "Every ACCU feature automatically targets the connected device:",
            code  = """Shell terminal     → commands run on TARGET
App Freeze         → freezes TARGET apps
Debloat            → removes TARGET bloatware
Permission toggle  → changes TARGET app permissions
File chmod         → chmod on TARGET filesystem
Diagnostics        → shows TARGET device info (uid=2000)

Verify: ACCU Shell → type: adb shell getprop ro.product.model
→ shows TARGET phone model""",
        ),
        TutorialStep(
            title = "Bonus: Upgrade to wireless after OTG connects",
            body  = "Once OTG is connected you can switch to wireless so you can remove the cable:",
            code  = """In ACCU Shell → type these commands while OTG is active:

  adb tcpip 5555
  → \"restarting in TCP mode port: 5555\"

  adb shell ip route
  → Note the TARGET IP (e.g. 192.168.1.42)

  Disconnect the OTG cable, then:
  adb connect 192.168.1.42:5555

OR: use the Wireless ADB flow in ACCU Center
  → tap \"Wireless ADB\" → auto-discovers the target""",
            tip   = "This is useful for setting up wireless ADB on older Android 10 devices that don't have the Wireless Debugging menu.",
        ),
        TutorialStep(
            title = "Disconnect",
            body  = "When done:",
            code  = """ACCU Center → tap \"Stop\"
→ Clears the OTG session

Or unplug the cable — ACCU detects disconnection and updates the status card.""",
        ),
    )
)

private val FEATURES_TUTORIAL = TutorialSection(
    title    = "All ADB Features in ACCU",
    subtitle = "Complete reference of everything you can do once connected",
    icon     = Icons.Default.List,
    color    = AccentGreen,
    steps    = listOf(
        TutorialStep(
            title = "ADB Shell — Interactive Terminal",
            body  = "ACCU → Shell\nFull terminal with 3 modes: Local (ACCU), Wi-Fi ADB, OTG ADB.",
            code  = """Features:
  • Run any ADB / shell command
  • Command history (↑/↓ keys)
  • Bookmarks for frequent commands
  • AI-powered command suggestions
  • Output search (Ctrl+F)
  • Save output to file
  • 200+ preloaded command examples
  • Script editor & runner""",
        ),
        TutorialStep(
            title = "App Management",
            body  = "ACCU → App Manager",
            code  = """Commands available:
  adb shell pm list packages -3         # User apps
  adb shell pm list packages -s         # System apps
  adb shell pm uninstall --user 0 <pkg> # Remove bloat
  adb shell pm disable-user <pkg>       # Disable
  adb shell pm enable <pkg>             # Enable
  adb shell pm clear <pkg>              # Clear data
  adb shell am force-stop <pkg>         # Force stop
  adb shell monkey -p <pkg> 1           # Launch app
  adb shell pm path <pkg>               # APK path
  adb pull <apk_path>                   # Extract APK""",
        ),
        TutorialStep(
            title = "Package Information",
            body  = "ACCU → App Manager → App detail",
            code  = """adb shell dumpsys package <pkg>    # Full info
adb shell pm list permissions <pkg> # Permissions
adb shell pm list instrumentation   # Test runners
adb shell pm dump <pkg> | grep versionName  # Version
adb shell pm dump <pkg> | grep firstInstallTime""",
        ),
        TutorialStep(
            title = "File Management",
            body  = "ACCU → Shell → File Browser (folder icon when connected)",
            code  = """adb shell ls -la /sdcard/          # List files
adb shell ls -la /sdcard/Android/data/
adb push local.txt /sdcard/        # Upload
adb pull /sdcard/file.txt          # Download
adb shell rm /sdcard/file.txt      # Delete
adb shell mkdir /sdcard/new_folder # Create folder
adb shell mv /sdcard/a.txt /sdcard/b.txt  # Rename
adb shell cp /sdcard/src.txt /sdcard/dst.txt""",
        ),
        TutorialStep(
            title = "Process Manager",
            body  = "ACCU → Shell → Processes (from Shell top bar or ADB Hub)",
            code  = """adb shell ps -A                    # All processes
adb shell top -b -n1               # CPU/RAM usage
adb shell kill <PID>               # Kill process
adb shell dumpsys activity services # Active services
adb shell am kill <package>         # Kill app process""",
        ),
        TutorialStep(
            title = "Logcat",
            body  = "ACCU → Shell → Logcat",
            code  = """adb logcat                        # All logs
adb logcat -v time                 # With timestamps
adb logcat ActivityManager:I *:S   # Filter by tag/level
adb logcat | grep -i "error"       # Search
adb logcat -d > log.txt            # Save to file
adb logcat -c                      # Clear log buffer""",
        ),
        TutorialStep(
            title = "Screenshots & Screen Recording",
            body  = "ACCU → Shell → Screen Capture",
            code  = """# Screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png
adb shell rm /sdcard/screen.png

# Screen recording (mp4, max 3 min)
adb shell screenrecord /sdcard/record.mp4
# Press Ctrl+C to stop, then:
adb pull /sdcard/record.mp4

# Custom options:
adb shell screenrecord --size 1280x720 --bit-rate 4000000 /sdcard/record.mp4""",
        ),
        TutorialStep(
            title = "Device Information",
            body  = "ACCU → Shell → Device Info",
            code  = """adb shell getprop                 # All properties
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell cat /proc/meminfo
adb shell cat /proc/cpuinfo
adb shell dumpsys battery
adb shell wm size && wm density""",
        ),
        TutorialStep(
            title = "Fastboot & Device Control",
            body  = "ACCU → Shell → Fastboot",
            code  = """adb reboot                        # Normal reboot
adb reboot bootloader             # Bootloader mode
adb reboot recovery               # Recovery mode
adb shell input keyevent 26       # Power button
adb shell input keyevent 224      # Wake screen
adb shell input tap 540 960       # Tap at x,y
adb shell input text "hello"      # Type text
fastboot devices                  # In bootloader
fastboot reboot                   # Exit fastboot""",
        ),
        TutorialStep(
            title = "Permissions, Settings & Power-User Commands",
            body  = "All available via ACCU Shell → Command Examples",
            code  = """# Grant/Revoke permissions
adb shell pm grant <pkg> android.permission.READ_CONTACTS
adb shell pm revoke <pkg> android.permission.CAMERA

# Settings database
adb shell settings get global airplane_mode_on
adb shell settings put global airplane_mode_on 1
adb shell settings put secure install_non_market_apps 1

# Activity manager
adb shell am start -a android.intent.action.VIEW -d "https://example.com"
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
adb shell am start-activity -n com.pkg/.HiddenActivity

# Animation scale (speed up / disable)
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0""",
        ),
    )
)

private val TROUBLESHOOT_TUTORIAL = TutorialSection(
    title    = "Troubleshooting",
    subtitle = "Common problems and how to fix them",
    icon     = Icons.Default.BugReport,
    color    = AccentRed,
    steps    = listOf(
        TutorialStep(
            title   = "\"no devices/emulators found\"",
            body    = "ADB cannot see the connected device.",
            code    = """Checks:
  1. USB debugging is enabled on target (Developer Options)
  2. USB cable supports data (not charge-only)
  3. Target screen is unlocked when connecting
  4. Check USB notification on target → \"File Transfer\" mode
  5. Run: adb kill-server && adb start-server
  6. Try a different USB port / cable

For OTG:
  7. Host phone supports USB OTG host mode
  8. OTG adapter (not a regular cable) is used""",
            warning = "Charge-only USB-C cables are very common. Always test with a known-good data cable.",
        ),
        TutorialStep(
            title = "\"unauthorized\" after adb devices",
            body  = "The target device hasn't approved the debug connection.",
            code  = """Fix:
  1. Look at target phone screen for an RSA dialog
  2. Check notification shade
  3. Tap [Allow]

If dialog doesn't appear:
  1. adb kill-server
  2. Delete ~/.android/adbkey (host side, if on PC)
  3. On target: Developer Options → Revoke USB debugging authorizations
  4. Reconnect""",
        ),
        TutorialStep(
            title = "Wi-Fi ADB: \"Connection refused\"",
            body  = "Cannot connect wirelessly to target.",
            code  = """Checks:
  1. Both phones on same Wi-Fi? (same SSID/router)
  2. IP address correct? Run: ip addr show wlan0
  3. Wireless debugging still enabled on target?
  4. Firewall/VPN on host phone interfering?
  5. Try: adb disconnect  (disconnect all first)
     Then: adb connect <IP>:<PORT>
  6. Port number correct? Get from Developer Options → Wireless debugging
  7. Phone went to sleep? Wake target & reconnect""",
        ),
        TutorialStep(
            title = "Pairing fails / wrong code",
            body  = "The 6-digit pairing code doesn't work.",
            code  = """Fix:
  1. Code expires after ~60 seconds — request a new one
  2. Use pairing PORT (not connection port — they're different)
  3. Both devices must be on same Wi-Fi network
  4. Disable VPN on both phones
  5. Retry: generate a fresh code on target, use immediately""",
        ),
        TutorialStep(
            title = "adb tcpip 5555 fails",
            body  = "Cannot switch to TCP mode.",
            code  = """Fix:
  1. Device must be detected first: adb devices → shows device
  2. Run: adb tcpip 5555 (wait for \"restarting in TCP mode\" message)
  3. Unplug cable, wait 3 seconds
  4. Run: adb connect <device_IP>:5555
  5. If it disconnects on sleep: adb shell svc wifi enable""",
        ),
        TutorialStep(
            title = "Permission denied for certain commands",
            body  = "Some ADB commands require root or elevated privileges.",
            code  = """Workaround options:
  1. Enable ACCU System Service → higher privilege
  2. For pm commands with --user 0: may need system-level ADB
  3. Check if device has a custom ROM with relaxed permissions
  4. Some commands need root: su -c "command"
  5. Grant INSTALL_PACKAGES via:
     adb shell pm grant com.accu android.permission.INSTALL_PACKAGES""",
        ),
    )
)

private val ALL_SECTIONS = listOf(WIFI_ADB_TUTORIAL, OTG_ADB_TUTORIAL, FEATURES_TUTORIAL, TROUBLESHOOT_TUTORIAL)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbTutorialScreen(onBack: () -> Unit = {}) {
    val clipboard = LocalClipboardManager.current
    var selectedSection by remember { mutableIntStateOf(0) }
    val section = ALL_SECTIONS[selectedSection]
    val expandedSteps = remember { mutableStateMapOf<Int, Boolean>().also { map -> ALL_SECTIONS[0].steps.indices.forEach { map[it] = true } } }

    Scaffold(
        topBar = {
            ACCTopBar(
                title = "ADB Connection Guide",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // Section selector
            item {
                Column(Modifier.fillMaxWidth()) {
                    ALL_SECTIONS.forEachIndexed { idx, sec ->
                        SectionSelectorRow(sec, idx == selectedSection, sec.color, onClick = {
                            selectedSection = idx
                            expandedSteps.clear()
                            sec.steps.indices.forEach { expandedSteps[it] = idx == 0 || idx == 1 }
                        })
                    }
                    HorizontalDivider()
                }
            }

            // Section header
            item {
                Surface(color = section.color.copy(0.08f)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = section.color.copy(0.15f), modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(section.icon, null, Modifier.size(28.dp), tint = section.color) }
                        }
                        Column {
                            Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = section.color)
                            Text(section.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Steps
            section.steps.forEachIndexed { idx, step ->
                item(key = "${selectedSection}_$idx") {
                    TutorialStepCard(
                        step = step,
                        stepNumber = idx + 1,
                        sectionColor = section.color,
                        expanded = expandedSteps[idx] != false,
                        onToggle = { expandedSteps[idx] = expandedSteps[idx] == false },
                        clipboard = clipboard,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionSelectorRow(section: TutorialSection, selected: Boolean, color: Color, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(section.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) color else MaterialTheme.colorScheme.onSurface) },
        supportingContent = { Text(section.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
        leadingContent = {
            Surface(shape = CircleShape, color = if (selected) color.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(section.icon, null, Modifier.size(20.dp), tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        trailingContent = { if (selected) Icon(Icons.Default.ChevronRight, null, tint = color) },
        colors = ListItemDefaults.colors(containerColor = if (selected) color.copy(0.04f) else MaterialTheme.colorScheme.surface),
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun TutorialStepCard(
    step: TutorialStep,
    stepNumber: Int,
    sectionColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = CircleShape, color = sectionColor.copy(0.15f), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$stepNumber", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = sectionColor)
                }
            }
            Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step.body.isNotBlank()) {
                    Text(step.body, style = MaterialTheme.typography.bodyMedium)
                }
                step.code?.let { code ->
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Command", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.ContentCopy, "Copy", Modifier.size(14.dp))
                                }
                            }
                            Text(code, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
                step.tip?.let { tip ->
                    Surface(shape = RoundedCornerShape(8.dp), color = AccentGreen.copy(0.08f)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lightbulb, null, Modifier.size(16.dp), tint = AccentGreen)
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                step.warning?.let { warn ->
                    Surface(shape = RoundedCornerShape(8.dp), color = AccentRed.copy(0.08f)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = AccentRed)
                            Text(warn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    }
}
