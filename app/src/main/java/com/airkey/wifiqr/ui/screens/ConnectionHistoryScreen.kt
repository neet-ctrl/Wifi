package com.airkey.wifiqr.ui.screens

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.data.ConnectionEvent
import com.airkey.wifiqr.data.WifiNetwork
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ConnectionHistoryScreen(
    network: WifiNetwork,
    viewModel: WifiViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.getEventsForNetwork(network.id).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val shortDate = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    val totalConnections = events.size
    val avgDownload = events.mapNotNull { it.downloadSpeedMbps }.average().takeIf { !it.isNaN() }
    val avgUpload = events.mapNotNull { it.uploadSpeedMbps }.average().takeIf { !it.isNaN() }
    val avgPing = events.mapNotNull { it.pingMs }.average().takeIf { !it.isNaN() }
    val bestSignal = events.mapNotNull { it.signalDbm }.maxOrNull()
    val lastSeven = remember(events) {
        val days = (0..6).map { d ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -d)
            val dayStart = Calendar.getInstance().apply {
                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            val count = events.count { it.connectedAt in dayStart until dayEnd }
            Pair(dayFormat.format(Date(dayStart)).take(1), count)
        }.reversed()
        days
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
                Text("Connection History", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(network.ssid, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
            }
            Box(
                modifier = Modifier
                    .background(NeonPurple.copy(0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, NeonPurple.copy(0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("$totalConnections connects", style = MaterialTheme.typography.labelMedium, color = NeonPurple, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (lastSeven.any { it.second > 0 }) {
                Spacer(Modifier.height(8.dp))
                Text("Last 7 Days", style = MaterialTheme.typography.labelLarge, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                BarChart(lastSeven)
                Spacer(Modifier.height(16.dp))
            }

            if (avgDownload != null || avgPing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (avgDownload != null) StatCard("Avg Download", "${avgDownload.roundToInt()} Mbps", Icons.Rounded.Download, NeonCyan, Modifier.weight(1f))
                    if (avgUpload != null) StatCard("Avg Upload", "${avgUpload!!.roundToInt()} Mbps", Icons.Rounded.Upload, NeonPurple, Modifier.weight(1f))
                    if (avgPing != null) StatCard("Avg Ping", "${avgPing.roundToInt()} ms", Icons.Rounded.Timer, OrangeWarn, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (bestSignal != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Best Signal", "$bestSignal dBm", Icons.Rounded.SignalCellularAlt, GreenSuccess, Modifier.weight(1f))
                    StatCard("Total Sessions", "$totalConnections", Icons.Rounded.Bolt, NeonPurple, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.History, null, tint = TextMuted, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No connection history yet", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("Events are logged when you use Connect Instantly", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Text("All Sessions", style = MaterialTheme.typography.labelLarge, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                events.forEachIndexed { i, event ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(i.coerceAtMost(10) * 40L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
                    ) {
                        HistoryEventCard(event, dateFormat)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun BarChart(data: List<Pair<String, Int>>) {
    val maxVal = data.maxOf { it.second }.coerceAtLeast(1)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(GlassWhite, RoundedCornerShape(16.dp))
            .border(1.dp, GlassWhite2, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (day, count) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    if (count > 0) {
                        Text("$count", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(count.toFloat() / maxVal.toFloat() * 0.75f + 0.05f)
                            .background(
                                Brush.verticalGradient(listOf(NeonCyan, NeonPurple)),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(day, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(GlassWhite, RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun HistoryEventCard(event: ConnectionEvent, dateFormat: SimpleDateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(14.dp))
            .border(1.dp, GlassWhite2, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(NeonPurple.copy(0.15f), CircleShape)
                .border(1.dp, NeonPurple.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bolt, null, tint = NeonPurple, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(dateFormat.format(Date(event.connectedAt)), style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
            if (event.downloadSpeedMbps != null || event.signalDbm != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    event.downloadSpeedMbps?.let {
                        Text("↓ ${it.roundToInt()} Mbps", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                    event.uploadSpeedMbps?.let {
                        Text("↑ ${it.roundToInt()} Mbps", style = MaterialTheme.typography.labelSmall, color = NeonPurple)
                    }
                    event.pingMs?.let {
                        Text("${it}ms", style = MaterialTheme.typography.labelSmall, color = OrangeWarn)
                    }
                    event.signalDbm?.let {
                        Text("$it dBm", style = MaterialTheme.typography.labelSmall, color = GreenSuccess)
                    }
                }
            } else {
                Text("Connection logged", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}
