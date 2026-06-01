package com.airkey.wifiqr.ui.screens

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import kotlin.math.*

@Composable
fun HomeScreen(
    viewModel: WifiViewModel,
    onNavigateScan: () -> Unit,
    onNavigateGenerate: () -> Unit,
    onNavigateSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "anim"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAnimatedBackground(this, animOffset)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(listOf(NeonPurple.copy(alpha = 0.3f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "AirKey",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                )
            )
            Text(
                "Your WiFi QR Universe",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(36.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Networks", "${uiState.networkCount}", Icons.Rounded.Storage, Modifier.weight(1f))
                StatCard("Secured", "256-bit", Icons.Rounded.Lock, Modifier.weight(1f))
                StatCard("Instant", "Connect", Icons.Rounded.Bolt, Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            ActionCard(
                title = "Scan WiFi QR Code",
                subtitle = "Extract SSID, password & all details instantly",
                icon = Icons.Rounded.QrCodeScanner,
                gradientColors = listOf(NeonPurple, Color(0xFF9C27B0)),
                onClick = onNavigateScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = "Generate QR",
                    subtitle = "Create stunning custom codes",
                    icon = Icons.Rounded.QrCode2,
                    gradientColors = listOf(Color(0xFF00C9FF), NeonCyan),
                    onClick = onNavigateGenerate,
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    title = "Saved WiFi",
                    subtitle = "Your network vault",
                    icon = Icons.Rounded.Bookmark,
                    gradientColors = listOf(NeonPink, Color(0xFFFF6B35)),
                    onClick = onNavigateSaved,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Features",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureRow(Icons.Rounded.FlashOn, "Instant decode", "Extract all WiFi details from any QR code", NeonPurple)
                FeatureRow(Icons.Rounded.Palette, "Designer QR codes", "Gradients, logos, dots, frames & shapes", NeonCyan)
                FeatureRow(Icons.Rounded.CloudDone, "Permanent storage", "Never lose a WiFi password again", GreenSuccess)
                FeatureRow(Icons.Rounded.Share, "1-tap share", "Share QR codes that connect instantly", NeonPink)
                FeatureRow(Icons.Rounded.Security, "Secure vault", "All networks saved privately on-device", OrangeWarn)
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(GlassWhite, RoundedCornerShape(16.dp))
            .border(1.dp, GlassWhite2, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "scale")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable { pressed = true; onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(150)
            pressed = false
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

private fun drawAnimatedBackground(scope: DrawScope, t: Float) {
    with(scope) {
        val w = size.width
        val h = size.height
        val t2pi = t * 2 * PI.toFloat()

        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x206C63FF), Color.Transparent),
                center = Offset(w * 0.2f + w * 0.15f * sin(t2pi), h * 0.3f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.2f + w * 0.15f * sin(t2pi), h * 0.3f)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x1500F5FF), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.6f + h * 0.1f * cos(t2pi)),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.8f, h * 0.6f + h * 0.1f * cos(t2pi))
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x18FF006E), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.85f),
                radius = w * 0.4f
            ),
            radius = w * 0.4f,
            center = Offset(w * 0.5f, h * 0.85f)
        )
    }
}
