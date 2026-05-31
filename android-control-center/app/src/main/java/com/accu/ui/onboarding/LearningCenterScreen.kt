package com.accu.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.accu.ui.components.ACCTopBar

data class Guide(val title: String, val description: String, val steps: List<String>, val icon: androidx.compose.ui.graphics.vector.ImageVector, val difficulty: String)

val guides = listOf(
    Guide(
        "Setting Up ACCU Connection",
        "Enable elevated ADB access without root using ACCU's built-in wireless ADB",
        listOf(
            "Enable Developer Options: Settings → About Phone → tap Build Number 7 times",
            "Enable Wireless Debugging: Settings → Developer Options → Wireless debugging",
            "Open ACCU → Settings → ACCU Center → tap 'Start Pairing Discovery'",
            "On your device, tap 'Pair device with pairing code'",
            "Enter the 6-digit pairing code shown on your device",
            "ACCU connects automatically — ACCU Center should show 'Active'",
        ),
        Icons.Default.Hub, "Beginner",
    ),
    Guide(
        "Debloating Your Device",
        "Safely remove Samsung, Google, and carrier bloatware",
        listOf(
            "Ensure ACCU is connected (ACC will show 'Active')",
            "Go to App Manager → Debloat tab",
            "Select a preset category (Google, Samsung, Carrier Bloat)",
            "Review the list — uncheck apps you want to keep",
            "Tap 'Remove for User' — this is reversible",
            "If an app breaks something, use 'Reinstall for User' to restore",
            "For complete removal (irreversible), use root mode",
        ),
        Icons.Default.Delete, "Intermediate",
    ),
    Guide(
        "Blocking Trackers & Privacy",
        "Block analytics, ads, and trackers at the component level",
        listOf(
            "Go to Privacy Center → Trackers tab",
            "Tap 'Block All' for each tracker category you want to block",
            "ACC uses Intent Firewall (IFW) for invisible blocking",
            "Trackers cannot detect they are blocked — more effective than hosts blocking",
            "Check Component Manager to see all blocked components",
            "Unblock individual components from Component Manager",
        ),
        Icons.Default.Security, "Beginner",
    ),
    Guide(
        "Using the Audio DSP Engine",
        "Enhance audio quality with RootlessJamesDSP",
        listOf(
            "Go to Audio Center",
            "Toggle the DSP engine ON",
            "Select a built-in preset or create your own",
            "Adjust the 10-band equalizer to your taste",
            "Enable Bass Boost for deeper lows",
            "Try AutoEQ profiles for headphone correction",
            "For advanced users: use Liveprog EEL2 scripts",
            "Save your settings as a custom preset",
        ),
        Icons.Default.Equalizer, "Beginner",
    ),
    Guide(
        "Setting Up Call Recording",
        "Record calls rootlessly via ACCU + scrcpy",
        listOf(
            "Ensure ACCU is connected",
            "Go to Call Recorder",
            "Toggle 'Call Recording' ON",
            "Select audio source (VOICE_CALL recommended)",
            "Choose format (AAC for small size, PCM for best quality)",
            "Make a test call — recording starts automatically",
            "Access recordings in the Call Recorder screen",
        ),
        Icons.Default.Call, "Intermediate",
    ),
    Guide(
        "Network Toggles Without Root",
        "Control Wi-Fi, mobile data, and more via ACCU",
        listOf(
            "Ensure ACCU is connected",
            "Go to Network Center",
            "Use the toggle tiles to control Wi-Fi, data, BT, NFC",
            "ACC uses 'svc' command via ACCU for rootless toggles",
            "Add ACC Quick Settings tiles via Network Center → QS Tiles",
            "For wireless ADB setup, use ACCU Center → Wireless ADB",
        ),
        Icons.Default.Wifi, "Beginner",
    ),
    Guide(
        "Key Mapper: Remapping Buttons",
        "Assign custom actions to volume buttons, power, and more",
        listOf(
            "Go to Automation → Key Mapper tab",
            "Tap + to create a new mapping",
            "Select the trigger key (e.g., Volume Up long press)",
            "Add an action (e.g., Play/Pause media)",
            "Add constraints if needed (e.g., only when music playing)",
            "Enable the mapping with the toggle switch",
            "Ensure ACC Accessibility Service is enabled for key events",
        ),
        Icons.Default.Keyboard, "Intermediate",
    ),
    Guide(
        "Per-App Language Selection",
        "Set individual languages for each app",
        listOf(
            "Go to Language Center",
            "Search for the app you want to change",
            "Tap the language icon on the right",
            "Select the desired language from the list",
            "The app will use the selected language on next launch",
            "Works on Android 13+ natively; older versions use ACCU",
            "Reset to system language by tapping the reset icon",
        ),
        Icons.Default.Language, "Beginner",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningCenterScreen(onBack: () -> Unit) {
    var expandedGuide by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { ACCTopBar(title = "Learning Center", onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Guides & Tutorials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${guides.size} guides available — from beginner to advanced", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            items(guides, key = { it.title }) { guide ->
                Card(
                    onClick = { expandedGuide = if (expandedGuide == guide.title) null else guide.title },
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(guide.icon, null, modifier = Modifier.size(22.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(guide.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(guide.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                color = when(guide.difficulty) {
                                    "Beginner"     -> androidx.compose.ui.graphics.Color(0xFF00E676).copy(0.2f)
                                    "Intermediate" -> androidx.compose.ui.graphics.Color(0xFFFF6D00).copy(0.2f)
                                    else           -> MaterialTheme.colorScheme.errorContainer
                                },
                            ) {
                                Text(guide.difficulty, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Icon(if (expandedGuide == guide.title) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }

                        if (expandedGuide == guide.title) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            guide.steps.forEachIndexed { i, step ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${i+1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(step, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
