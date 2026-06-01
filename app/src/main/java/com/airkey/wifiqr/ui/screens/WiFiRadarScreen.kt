package com.airkey.wifiqr.ui.screens

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.data.WifiNetwork
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

data class RadarNetwork(
    val ssid: String,
    val bssid: String,
    val signalDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String,
    val isSaved: Boolean,
    val savedNetwork: WifiNetwork? = null
) {
    val signalPercent: Int get() = ((signalDbm + 100).coerceIn(0, 50) * 2).coerceIn(0, 100)
    val band: String get() = when {
        frequencyMhz < 3000 -> "2.4 GHz"
        frequencyMhz < 6000 -> "5 GHz"
        else -> "6 GHz"
    }
    val estimatedDistanceM: Double get() {
        val txPower = -40.0
        val n = 2.0
        return 10.0.pow((txPower - signalDbm) / (10.0 * n))
    }
    val security: String get() = when {
        capabilities.contains("WPA3") -> "WPA3"
        capabilities.contains("WPA2") -> "WPA2"
        capabilities.contains("WPA") -> "WPA"
        capabilities.contains("WEP") -> "WEP"
        else -> "Open"
    }
}

@Composable
fun WiFiRadarScreen(
    viewModel: WifiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var networks by remember { mutableStateOf<List<RadarNetwork>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var lastScanTime by remember { mutableLongStateOf(0L) }
    var filterSaved by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("signal") }

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweep by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infiniteTransition.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    fun scan() {
        scope.launch {
            isScanning = true
            val results = withContext(Dispatchers.IO) {
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wm.startScan()
                    delay(1500)
                    wm.scanResults ?: emptyList()
                } catch (_: Exception) { emptyList<ScanResult>() }
            }
            val savedMap = uiState.networks.associateBy { it.ssid }
            networks = results
                .filter { it.SSID?.isNotBlank() == true }
                .map { r ->
                    val saved = savedMap[r.SSID]
                    RadarNetwork(
                        ssid = r.SSID ?: "",
                        bssid = r.BSSID ?: "",
                        signalDbm = r.level,
                        frequencyMhz = r.frequency,
                        capabilities = r.capabilities ?: "",
                        isSaved = saved != null,
                        savedNetwork = saved
                    )
                }
                .distinctBy { it.ssid }
            lastScanTime = System.currentTimeMillis()
            isScanning = false
        }
    }

    LaunchedEffect(Unit) { scan() }

    val displayed = remember(networks, filterSaved, sortBy) {
        var list = if (filterSaved) networks.filter { it.isSaved } else networks
        list = when (sortBy) {
            "signal" -> list.sortedByDescending { it.signalDbm }
            "name" -> list.sortedBy { it.ssid }
            "saved" -> list.sortedByDescending { it.isSaved }
            else -> list
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(GlassWhite, CircleShape)) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("WiFi Radar", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    if (networks.isNotEmpty()) "${networks.size} networks found · ${networks.count { it.isSaved }} saved" else "Tap scan to detect networks",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                )
            }
            IconButton(
                onClick = { scan() },
                modifier = Modifier.background(NeonPurple.copy(0.2f), CircleShape)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = NeonPurple,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Rounded.Radar, null, tint = NeonPurple)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val c = center
                val r = size.minDimension / 2f
                for (i in 1..4) {
                    drawCircle(
                        color = Color(0xFF6C63FF).copy(0.08f + i * 0.04f),
                        radius = r * i / 4f,
                        style = Stroke(1f)
                    )
                }
                drawLine(Color(0xFF6C63FF).copy(0.15f), c.copy(x = 0f), c.copy(x = size.width), strokeWidth = 1f)
                drawLine(Color(0xFF6C63FF).copy(0.15f), c.copy(y = 0f), c.copy(y = size.height), strokeWidth = 1f)
                if (isScanning) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Color(0xFF6C63FF).copy(0.6f), Color.Transparent),
                            c
                        ),
                        startAngle = sweep,
                        sweepAngle = 90f,
                        useCenter = true,
                        alpha = pulse
                    )
                }
                displayed.take(20).forEachIndexed { i, net ->
                    val angle = (i.toFloat() / displayed.size.coerceAtLeast(1) * 360f) * (Math.PI / 180.0)
                    val dist = (1f - net.signalPercent / 100f).coerceIn(0.15f, 0.90f)
                    val px = (c.x + r * dist * Math.cos(angle)).toFloat()
                    val py = (c.y + r * dist * Math.sin(angle)).toFloat()
                    val dotColor = if (net.isSaved) Color(0xFF00F5FF) else Color(0xFF6C63FF)
                    drawCircle(dotColor.copy(0.25f), radius = if (net.isSaved) 10f else 7f, center = androidx.compose.ui.geometry.Offset(px, py))
                    drawCircle(dotColor, radius = if (net.isSaved) 5f else 3f, center = androidx.compose.ui.geometry.Offset(px, py))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Router, null, tint = NeonPurple, modifier = Modifier.size(28.dp))
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterSaved,
                onClick = { filterSaved = !filterSaved },
                label = { Text("Saved Only") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(0.15f),
                    selectedLabelColor = NeonCyan,
                    containerColor = GlassWhite, labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterSaved, selectedBorderColor = NeonCyan, borderColor = GlassWhite2),
                shape = RoundedCornerShape(12.dp)
            )
            listOf("signal" to "Signal", "name" to "Name", "saved" to "Saved").forEach { (key, label) ->
                FilterChip(
                    selected = sortBy == key,
                    onClick = { sortBy = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple.copy(0.15f), selectedLabelColor = NeonPurple,
                        containerColor = GlassWhite, labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = sortBy == key, selectedBorderColor = NeonPurple, borderColor = GlassWhite2),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (displayed.isEmpty() && !isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.WifiOff, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No networks found", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("Enable WiFi and grant Location permission", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(displayed, key = { _, n -> n.bssid }) { index, net ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index.coerceAtMost(8) * 40L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
                    ) {
                        RadarNetworkCard(net)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun RadarNetworkCard(net: RadarNetwork) {
    val borderColor = if (net.isSaved) NeonCyan else GlassWhite2
    val secColor = when (net.security) {
        "WPA3" -> GreenSuccess; "WPA2", "WPA" -> NeonCyan; "WEP" -> OrangeWarn; else -> RedError
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (net.isSaved)
                    Brush.linearGradient(listOf(NeonCyan.copy(0.05f), GlassWhite))
                else Brush.linearGradient(listOf(GlassWhite, GlassWhite))
            )
            .border(
                if (net.isSaved) 1.5.dp else 1.dp,
                if (net.isSaved) Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                else Brush.linearGradient(listOf(GlassWhite2, GlassWhite2)),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SignalBars(net.signalPercent, if (net.isSaved) NeonCyan else NeonPurple)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(net.ssid, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (net.isSaved) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(NeonCyan.copy(0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("Saved", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${net.signalDbm} dBm", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("·", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(net.band, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("·", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(net.security, style = MaterialTheme.typography.labelSmall, color = secColor)
                }
                val dist = net.estimatedDistanceM
                Text(
                    when {
                        dist < 2 -> "< 2m away"
                        dist < 10 -> "~${dist.toInt()}m away"
                        dist < 30 -> "~${dist.toInt()}m away"
                        else -> "> 30m away"
                    },
                    style = MaterialTheme.typography.labelSmall, color = TextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(secColor.copy(0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${net.signalPercent}%", style = MaterialTheme.typography.labelSmall, color = secColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SignalBars(percent: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.width(24.dp).height(24.dp)
    ) {
        val bars = 4
        val active = ((percent / 100f) * bars).toInt().coerceIn(0, bars)
        for (i in 0 until bars) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(0.3f + i * 0.175f)
                    .background(
                        if (i < active) color else TextMuted.copy(0.3f),
                        RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                    )
            )
        }
    }
}
