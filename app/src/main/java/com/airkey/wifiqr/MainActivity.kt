package com.airkey.wifiqr

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.airkey.wifiqr.data.WifiNetwork
import com.airkey.wifiqr.ui.screens.*
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Generate : Screen("generate") {
        fun createRoute(ssid: String = "", password: String = "", security: String = "", hidden: Boolean = false) =
            "generate?ssid=${encode(ssid)}&password=${encode(password)}&security=$security&hidden=$hidden"
        private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
    }
    object Saved : Screen("saved")
    object Settings : Screen("settings")
    object SpeedTest : Screen("speed_test")
    object WiFiRadar : Screen("wifi_radar")
    object NfcWriter : Screen("nfc_writer")
    object ConnectionHistory : Screen("connection_history")
}

class MainActivity : ComponentActivity() {
    private val viewModel: WifiViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private val _discoveredTag = mutableStateOf<Tag?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        var splashVisible = true
        splashScreen.setKeepOnScreenCondition { splashVisible }

        setContent {
            AirKeyTheme {
                LaunchedEffect(Unit) {
                    delay(1200)
                    splashVisible = false
                }
                AirKeyApp(viewModel, discoveredTag = _discoveredTag.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_MUTABLE else 0
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
                try {
                    adapter.enableForegroundDispatch(this, pendingIntent, null, null)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try { nfcAdapter?.disableForegroundDispatch(this) } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            _discoveredTag.value = tag
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AirKeyApp(viewModel: WifiViewModel, discoveredTag: Tag? = null) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")

    val showBottomBar = currentRoute in listOf("home", "scan", "saved", "settings")

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    var startupPermissionsDone by remember { mutableStateOf(false) }
    var showStartupPermissions by remember {
        mutableStateOf(!cameraPermission.status.isGranted || !locationPermission.status.isGranted)
    }

    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(2500)
            viewModel.clearToast()
        }
    }

    if (showStartupPermissions && !startupPermissionsDone) {
        StartupPermissionsScreen(
            cameraPermission = cameraPermission,
            locationPermission = locationPermission,
            onContinue = { startupPermissionsDone = true; showStartupPermissions = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut(tween(200)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(200)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateGenerate = { navController.navigate(Screen.Generate.createRoute()) },
                    onNavigateSaved = { navController.navigate(Screen.Saved.route) },
                    onNavigateSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateGenerate = { network ->
                        navController.navigate(Screen.Generate.createRoute(
                            ssid = network.ssid, password = network.password,
                            security = network.securityType, hidden = network.isHidden
                        ))
                    }
                )
            }
            composable(
                route = "generate?ssid={ssid}&password={password}&security={security}&hidden={hidden}",
                arguments = listOf(
                    navArgument("ssid") { defaultValue = "" },
                    navArgument("password") { defaultValue = "" },
                    navArgument("security") { defaultValue = "WPA" },
                    navArgument("hidden") { defaultValue = "false" }
                )
            ) { backStackEntry ->
                val ssid = backStackEntry.arguments?.getString("ssid")?.let {
                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
                } ?: ""
                val password = backStackEntry.arguments?.getString("password")?.let {
                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
                } ?: ""
                val security = backStackEntry.arguments?.getString("security") ?: "WPA"
                val hidden = backStackEntry.arguments?.getString("hidden") == "true"
                val prefillNetwork = if (ssid.isNotEmpty()) {
                    WifiNetwork(ssid = ssid, password = password, securityType = security, isHidden = hidden)
                } else null
                GeneratorScreen(viewModel = viewModel, prefillNetwork = prefillNetwork, onBack = { navController.popBackStack() })
            }
            composable(Screen.Saved.route) {
                SavedNetworksScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateGenerate = { network ->
                        navController.navigate(Screen.Generate.createRoute(
                            ssid = network.ssid, password = network.password,
                            security = network.securityType, hidden = network.isHidden
                        ))
                    },
                    onNavigateSpeedTest = { network ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("speed_network", network)
                        navController.navigate(Screen.SpeedTest.route)
                    },
                    onNavigateRadar = { navController.navigate(Screen.WiFiRadar.route) },
                    onNavigateNfc = { network ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("nfc_network", network)
                        navController.navigate(Screen.NfcWriter.route)
                    },
                    onNavigateHistory = { network ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("history_network", network)
                        navController.navigate(Screen.ConnectionHistory.route)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Screen.SpeedTest.route) {
                val network = navController.previousBackStackEntry?.savedStateHandle?.get<WifiNetwork>("speed_network")
                SpeedTestScreen(viewModel = viewModel, network = network, onBack = { navController.popBackStack() })
            }
            composable(Screen.WiFiRadar.route) {
                WiFiRadarScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Screen.NfcWriter.route) {
                val network = navController.previousBackStackEntry?.savedStateHandle?.get<WifiNetwork>("nfc_network")
                if (network != null) {
                    NfcWriterScreen(network = network, discoveredTag = discoveredTag, onBack = { navController.popBackStack() })
                } else {
                    navController.popBackStack()
                }
            }
            composable(Screen.ConnectionHistory.route) {
                val network = navController.previousBackStackEntry?.savedStateHandle?.get<WifiNetwork>("history_network")
                if (network != null) {
                    ConnectionHistoryScreen(network = network, viewModel = viewModel, onBack = { navController.popBackStack() })
                } else {
                    navController.popBackStack()
                }
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            AirKeyBottomBar(currentRoute = currentRoute, onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        }

        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            modifier = Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(top = 8.dp),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(CardSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, GlassWhite2, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.toastMessage ?: "", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartupPermissionsScreen(
    cameraPermission: PermissionState,
    locationPermission: PermissionState,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val storageGranted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) android.os.Environment.isExternalStorageManager()
        else true
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack).systemBarsPadding(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(Brush.linearGradient(listOf(NeonPurple, NeonCyan)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Security, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Text("Permissions Needed", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                "AirKey needs a few permissions to scan QR codes, save networks, and store files on your device.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StartupPermRow("Camera", "Scan WiFi QR codes", cameraPermission.status.isGranted, NeonPurple, Icons.Rounded.CameraAlt) { cameraPermission.launchPermissionRequest() }
                StartupPermRow("Location", "Required for WiFi scanning on Android 8+", locationPermission.status.isGranted, NeonCyan, Icons.Rounded.LocationOn) { locationPermission.launchPermissionRequest() }
                StartupPermRow("Manage Files", "Save QR codes to any folder", storageGranted, NeonPink, Icons.Rounded.FolderOpen) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, android.net.Uri.parse("package:${context.packageName}")))
                        } catch (_: Exception) {}
                    }
                }
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(NeonPurple, NeonCyan)), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("Continue to AirKey", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            TextButton(onClick = onContinue) {
                Text("Skip for now", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StartupPermRow(title: String, description: String, granted: Boolean, color: Color, icon: ImageVector, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(14.dp))
            .border(1.dp, if (granted) color.copy(0.35f) else GlassWhite2, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).background(color.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        if (granted) Icon(Icons.Rounded.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(22.dp))
        else TextButton(onClick = onRequest, colors = ButtonDefaults.textButtonColors(contentColor = color), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
            Text("Grant", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AirKeyBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem("home", "Home", Icons.Rounded.Home),
        NavItem("scan", "Scan", Icons.Rounded.QrCodeScanner),
        NavItem("saved", "Saved", Icons.Rounded.Bookmark),
        NavItem("settings", "Settings", Icons.Rounded.Settings),
    )
    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(CardSurface.copy(alpha = 0.97f), RoundedCornerShape(28.dp))
                .border(width = 1.dp, brush = Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.4f))), shape = RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val scale by animateFloatAsState(if (isSelected) 1.08f else 1f, label = "scale")
                    Column(
                        modifier = Modifier.scale(scale).clip(RoundedCornerShape(20.dp))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onNavigate(item.route) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(
                                if (isSelected) Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)), CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, null, tint = if (isSelected) Color.White else TextMuted, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(item.label, style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) NeonCyan else TextMuted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)
