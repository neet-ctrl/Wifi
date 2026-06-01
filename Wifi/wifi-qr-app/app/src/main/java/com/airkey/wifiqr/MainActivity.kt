package com.airkey.wifiqr

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.airkey.wifiqr.data.WifiNetwork
import com.airkey.wifiqr.ui.screens.*
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
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
}

class MainActivity : ComponentActivity() {
    private val viewModel: WifiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var splashVisible = true
        splashScreen.setKeepOnScreenCondition { splashVisible }

        setContent {
            AirKeyTheme {
                LaunchedEffect(Unit) {
                    delay(1200)
                    splashVisible = false
                }
                AirKeyApp(viewModel)
            }
        }
    }
}

@Composable
fun AirKeyApp(viewModel: WifiViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")

    val showBottomBar = currentRoute in listOf("home", "scan", "saved")

    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(2500)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
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
                    onNavigateSaved = { navController.navigate(Screen.Saved.route) }
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateGenerate = { network ->
                        navController.navigate(
                            Screen.Generate.createRoute(
                                ssid = network.ssid,
                                password = network.password,
                                security = network.securityType,
                                hidden = network.isHidden
                            )
                        )
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
                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
                } ?: ""
                val password = backStackEntry.arguments?.getString("password")?.let {
                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
                } ?: ""
                val security = backStackEntry.arguments?.getString("security") ?: "WPA"
                val hidden = backStackEntry.arguments?.getString("hidden") == "true"
                val prefillNetwork = if (ssid.isNotEmpty()) {
                    WifiNetwork(ssid = ssid, password = password, securityType = security, isHidden = hidden)
                } else null
                GeneratorScreen(
                    viewModel = viewModel,
                    prefillNetwork = prefillNetwork,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Saved.route) {
                SavedNetworksScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateGenerate = { network ->
                        navController.navigate(
                            Screen.Generate.createRoute(
                                ssid = network.ssid,
                                password = network.password,
                                security = network.securityType,
                                hidden = network.isHidden
                            )
                        )
                    }
                )
            }
        }

        // Bottom navigation bar
        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            AirKeyBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Toast notification
        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(top = 8.dp),
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

@Composable
fun AirKeyBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem("home", "Home", Icons.Rounded.Home),
        NavItem("scan", "Scan", Icons.Rounded.QrCodeScanner),
        NavItem("saved", "Saved", Icons.Rounded.Bookmark),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface.copy(alpha = 0.97f), RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.4f))),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val scale by animateFloatAsState(if (isSelected) 1.08f else 1f, label = "scale")

                    Column(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onNavigate(item.route) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isSelected)
                                        Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                                    else
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                item.icon, null,
                                tint = if (isSelected) Color.White else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) NeonCyan else TextMuted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)
