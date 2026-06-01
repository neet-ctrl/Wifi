package com.airkey.wifiqr.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.data.ConnectionEvent
import com.airkey.wifiqr.data.WifiNetwork
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.math.roundToInt

enum class TestState { IDLE, PINGING, DOWNLOADING, UPLOADING, DONE, ERROR }

data class SpeedResult(
    val pingMs: Int = 0,
    val downloadMbps: Float = 0f,
    val uploadMbps: Float = 0f,
    val signalDbm: Int = 0,
    val frequencyMhz: Int = 0,
    val linkSpeedMbps: Int = 0,
    val bssid: String = "",
    val ssid: String = "",
    val channelWidth: String = "",
    val ip: String = ""
)

@Composable
fun SpeedTestScreen(
    viewModel: WifiViewModel,
    network: WifiNetwork?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var testState by remember { mutableStateOf(TestState.IDLE) }
    var result by remember { mutableStateOf(SpeedResult()) }
    var errorMsg by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var liveSpeed by remember { mutableFloatStateOf(0f) }

    val wifiInfo = remember { getWifiDetails(context) }

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarAngle by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "angle"
    )

    fun runTest() {
        scope.launch {
            testState = TestState.PINGING
            progress = 0f
            liveSpeed = 0f
            try {
                val ping = measurePing()
                progress = 0.25f
                testState = TestState.DOWNLOADING
                val (dl, dlLive) = measureDownload { liveSpeed = it; progress = 0.25f + it / 100f * 0.35f }
                progress = 0.60f
                testState = TestState.UPLOADING
                val (ul, _) = measureUpload { liveSpeed = it; progress = 0.60f + it / 100f * 0.35f }
                progress = 1f

                result = SpeedResult(
                    pingMs = ping,
                    downloadMbps = dl,
                    uploadMbps = ul,
                    signalDbm = wifiInfo.signalDbm,
                    frequencyMhz = wifiInfo.frequencyMhz,
                    linkSpeedMbps = wifiInfo.linkSpeedMbps,
                    bssid = wifiInfo.bssid,
                    ssid = wifiInfo.ssid,
                    channelWidth = wifiInfo.channelWidth,
                    ip = wifiInfo.ip
                )
                testState = TestState.DONE

                if (network != null) {
                    val eventId = viewModel.logConnectionEvent(network.id, network.ssid)
                    viewModel.updateConnectionEventSpeeds(eventId, dl, ul, ping, wifiInfo.signalDbm, wifiInfo.frequencyMhz, wifiInfo.linkSpeedMbps)
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "Test failed"
                testState = TestState.ERROR
            }
        }
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
                Text("WiFi Speed Test", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(if (wifiInfo.ssid.isNotEmpty()) "Connected to ${wifiInfo.ssid}" else "Not connected to WiFi",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                if (testState != TestState.IDLE && testState != TestState.DONE && testState != TestState.ERROR) {
                    Canvas(modifier = Modifier.size(260.dp)) {
                        drawCircle(color = Color(0xFF6C63FF).copy(0.08f), radius = size.minDimension / 2f)
                        drawArc(
                            color = Color(0xFF6C63FF),
                            startAngle = radarAngle,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (testState) {
                        TestState.IDLE -> {
                            Icon(Icons.Rounded.Speed, null, tint = NeonPurple, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Ready to Test", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap Start to measure speed", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        TestState.PINGING -> SpeedGauge("Ping", "ms", 0f, NeonCyan)
                        TestState.DOWNLOADING -> SpeedGauge("Download", "Mbps", liveSpeed, NeonPurple)
                        TestState.UPLOADING -> SpeedGauge("Upload", "Mbps", liveSpeed, NeonCyan)
                        TestState.DONE -> {
                            Icon(Icons.Rounded.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("${result.downloadMbps.roundToInt()}", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Mbps download", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        TestState.ERROR -> {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = RedError, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Test Failed", style = MaterialTheme.typography.titleMedium, color = RedError)
                            Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }

            if (testState != TestState.IDLE && testState != TestState.DONE && testState != TestState.ERROR) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = NeonPurple,
                    trackColor = GlassWhite
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (testState) {
                        TestState.PINGING -> "Measuring latency…"
                        TestState.DOWNLOADING -> "Testing download…"
                        TestState.UPLOADING -> "Testing upload…"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelMedium, color = TextMuted
                )
            }

            Spacer(Modifier.height(24.dp))

            if (testState == TestState.DONE) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpeedMetricCard("Download", "${result.downloadMbps.roundToInt()} Mbps", Icons.Rounded.Download, NeonCyan, Modifier.weight(1f))
                    SpeedMetricCard("Upload", "${result.uploadMbps.roundToInt()} Mbps", Icons.Rounded.Upload, NeonPurple, Modifier.weight(1f))
                    SpeedMetricCard("Ping", "${result.pingMs} ms", Icons.Rounded.Timer, if (result.pingMs < 30) GreenSuccess else if (result.pingMs < 80) OrangeWarn else RedError, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }

            AdvancedWifiDetailsCard(wifiInfo)

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .coloredShadow(NeonPurple, 14.dp, 16.dp, alpha = 0.5f)
            ) {
                Button(
                    onClick = { runTest() },
                    enabled = testState == TestState.IDLE || testState == TestState.DONE || testState == TestState.ERROR,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = GlassWhite
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                if (testState == TestState.IDLE || testState == TestState.DONE || testState == TestState.ERROR)
                                    Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                                else Brush.linearGradient(listOf(GlassWhite, GlassWhite)),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (testState == TestState.DONE) "Run Again" else "Start Speed Test",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SpeedGauge(label: String, unit: String, value: Float, color: Color) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = color)
    Spacer(Modifier.height(4.dp))
    Text("${value.roundToInt()}", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
    Text(unit, style = MaterialTheme.typography.bodySmall, color = TextMuted)
}

@Composable
private fun SpeedMetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(GlassWhite, RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun AdvancedWifiDetailsCard(info: WifiDetails) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(20.dp))
            .border(1.dp, GlassWhite2, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Wifi, null, tint = NeonPurple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Advanced WiFi Details", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
        Divider(color = GlassWhite2)
        DetailRow("Network (SSID)", info.ssid.ifEmpty { "—" })
        DetailRow("BSSID (Router MAC)", info.bssid.ifEmpty { "—" })
        DetailRow("Signal Strength", if (info.signalDbm != 0) "${info.signalDbm} dBm  ${signalLabel(info.signalDbm)}" else "—")
        DetailRow("Frequency", if (info.frequencyMhz != 0) "${info.frequencyMhz} MHz  (${if (info.frequencyMhz < 3000) "2.4 GHz" else if (info.frequencyMhz < 6000) "5 GHz" else "6 GHz"})" else "—")
        DetailRow("Channel", if (info.frequencyMhz != 0) "${frequencyToChannel(info.frequencyMhz)}" else "—")
        DetailRow("Link Speed", if (info.linkSpeedMbps != 0) "${info.linkSpeedMbps} Mbps" else "—")
        if (info.channelWidth.isNotEmpty()) DetailRow("Channel Width", info.channelWidth)
        DetailRow("IP Address", info.ip.ifEmpty { "—" })
        if (info.signalDbm != 0) {
            DetailRow("Est. Distance", estimateDistance(info.signalDbm))
            DetailRow("Signal Quality", "${signalQualityPercent(info.signalDbm)}%")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

private fun signalLabel(dbm: Int) = when {
    dbm >= -50 -> "Excellent"
    dbm >= -60 -> "Good"
    dbm >= -70 -> "Fair"
    dbm >= -80 -> "Weak"
    else -> "Very Weak"
}

private fun signalQualityPercent(dbm: Int): Int {
    return ((dbm + 100).coerceIn(0, 50) * 2).coerceIn(0, 100)
}

private fun estimateDistance(dbm: Int): String {
    val txPower = -40.0
    val n = 2.0
    val dist = Math.pow(10.0, (txPower - dbm) / (10.0 * n))
    return when {
        dist < 1.0 -> "< 1 m (very close)"
        dist < 5.0 -> "~${dist.toInt()} m (same room)"
        dist < 15.0 -> "~${dist.toInt()} m (nearby)"
        dist < 30.0 -> "~${dist.toInt()} m (distant)"
        else -> "> 30 m (very far)"
    }
}

private fun frequencyToChannel(freqMhz: Int): Int {
    return when {
        freqMhz == 2412 -> 1; freqMhz == 2417 -> 2; freqMhz == 2422 -> 3
        freqMhz == 2427 -> 4; freqMhz == 2432 -> 5; freqMhz == 2437 -> 6
        freqMhz == 2442 -> 7; freqMhz == 2447 -> 8; freqMhz == 2452 -> 9
        freqMhz == 2457 -> 10; freqMhz == 2462 -> 11; freqMhz == 2467 -> 12
        freqMhz == 2472 -> 13; freqMhz in 5170..5825 -> (freqMhz - 5000) / 5
        else -> 0
    }
}

data class WifiDetails(
    val ssid: String = "",
    val bssid: String = "",
    val signalDbm: Int = 0,
    val frequencyMhz: Int = 0,
    val linkSpeedMbps: Int = 0,
    val channelWidth: String = "",
    val ip: String = ""
)

fun getWifiDetails(context: Context): WifiDetails {
    return try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = wm.connectionInfo ?: return WifiDetails()
        val rawSsid = info.ssid ?: ""
        val ssid = rawSsid.removeSurrounding("\"")
        val ipInt = info.ipAddress
        val ip = "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt shr 24 and 0xff}"
        val channelWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val nc = cm.getNetworkCapabilities(cm.activeNetwork)
                val bandwidth = nc?.linkDownstreamBandwidthKbps ?: 0
                when {
                    bandwidth > 300_000 -> "160 MHz"
                    bandwidth > 150_000 -> "80 MHz"
                    bandwidth > 70_000 -> "40 MHz"
                    else -> "20 MHz"
                }
            } catch (_: Exception) { "" }
        } else ""
        WifiDetails(
            ssid = ssid,
            bssid = info.bssid ?: "",
            signalDbm = info.rssi,
            frequencyMhz = info.frequency,
            linkSpeedMbps = info.linkSpeed,
            channelWidth = channelWidth,
            ip = if (ipInt == 0) "" else ip
        )
    } catch (_: Exception) { WifiDetails() }
}

private suspend fun measurePing(): Int = withContext(Dispatchers.IO) {
    try {
        val pings = (1..3).map {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 53), 3000)
            }
            (System.currentTimeMillis() - start).toInt()
        }
        pings.min()
    } catch (_: Exception) { 999 }
}

private suspend fun measureDownload(onProgress: (Float) -> Unit): Pair<Float, Float> =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://speed.cloudflare.com/__down?bytes=5000000")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.connect()
            val start = System.currentTimeMillis()
            val buf = ByteArray(32768)
            var total = 0L
            val stream = conn.inputStream
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                total += n
                val elapsed = (System.currentTimeMillis() - start) / 1000.0
                val speedMbps = if (elapsed > 0) ((total * 8.0) / 1_000_000.0 / elapsed).toFloat() else 0f
                onProgress(speedMbps.coerceAtMost(100f))
            }
            conn.disconnect()
            val elapsed = (System.currentTimeMillis() - start) / 1000.0
            val speed = ((total * 8.0) / 1_000_000.0 / elapsed).toFloat()
            Pair(speed, speed)
        } catch (e: Exception) {
            Pair(0f, 0f)
        }
    }

private suspend fun measureUpload(onProgress: (Float) -> Unit): Pair<Float, Float> =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://speed.cloudflare.com/__up")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(2_000_000L)
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.connect()
            val start = System.currentTimeMillis()
            val out: OutputStream = conn.outputStream
            val buf = ByteArray(32768) { 65 }
            var written = 0L
            val total = 2_000_000L
            while (written < total) {
                val toWrite = minOf(buf.size.toLong(), total - written).toInt()
                out.write(buf, 0, toWrite)
                written += toWrite
                val elapsed = (System.currentTimeMillis() - start) / 1000.0
                val speedMbps = if (elapsed > 0) ((written * 8.0) / 1_000_000.0 / elapsed).toFloat() else 0f
                onProgress(speedMbps.coerceAtMost(100f))
            }
            out.flush()
            conn.responseCode
            conn.disconnect()
            val elapsed = (System.currentTimeMillis() - start) / 1000.0
            val speed = ((written * 8.0) / 1_000_000.0 / elapsed).toFloat()
            Pair(speed, speed)
        } catch (e: Exception) {
            Pair(0f, 0f)
        }
    }
