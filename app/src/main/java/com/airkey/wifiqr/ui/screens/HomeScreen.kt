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
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onNavigateSaved: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "anim"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "ring"
    )
    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ring2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawEnhancedBackground(this, animOffset)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo with rotating rings
            Box(contentAlignment = Alignment.Center) {
                // Outer slow ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale * 0.97f)
                        .graphicsLayer { rotationZ = ringRotation2 }
                        .border(
                            1.dp,
                            Brush.sweepGradient(listOf(Color.Transparent, NeonPink.copy(0.4f), Color.Transparent, NeonCyan.copy(0.3f), Color.Transparent)),
                            CircleShape
                        )
                )
                // Glow halo
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(listOf(NeonPurple.copy(alpha = 0.35f), Color.Transparent)),
                            CircleShape
                        )
                )
                // Inner rotating ring
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .graphicsLayer { rotationZ = ringRotation }
                        .border(
                            2.dp,
                            Brush.sweepGradient(listOf(Color.Transparent, NeonPurple, NeonCyan, Color.Transparent)),
                            CircleShape
                        )
                )
                // Logo circle
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .coloredShadow(NeonPurple, 44.dp, 28.dp, alpha = 0.7f)
                        .background(
                            Brush.linearGradient(listOf(NeonPurple, Color(0xFF9C27B0), NeonCyan)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner shine
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                Brush.verticalGradient(listOf(Color.White.copy(0.18f), Color.Transparent)),
                                CircleShape
                            )
                    )
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
                StatCard("Networks", "${uiState.networkCount}", Icons.Rounded.Storage, NeonCyan, Modifier.weight(1f))
                StatCard("Secured", "256-bit", Icons.Rounded.Lock, NeonPurple, Modifier.weight(1f))
                StatCard("Instant", "Connect", Icons.Rounded.Bolt, NeonPink, Modifier.weight(1f))
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
                glowColor = NeonPurple,
                onClick = onNavigateScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            ActionCard(
                title = "Generate QR",
                subtitle = "Create stunning custom QR codes",
                icon = Icons.Rounded.QrCode2,
                gradientColors = listOf(Color(0xFF00C9FF), NeonCyan),
                glowColor = NeonCyan,
                onClick = onNavigateGenerate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            ActionCard(
                title = "Saved WiFi",
                subtitle = "Your network vault & history",
                icon = Icons.Rounded.Bookmark,
                gradientColors = listOf(NeonPink, Color(0xFFFF6B35)),
                glowColor = NeonPink,
                onClick = onNavigateSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

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

        IconButton(
            onClick = onNavigateSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
                .background(GlassWhite, CircleShape)
                .border(1.dp, GlassWhite2, CircleShape)
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .coloredShadow(accentColor, 16.dp, 16.dp, alpha = 0.3f)
            .background(
                Brush.linearGradient(listOf(CardSurface, DarkSurface)),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(accentColor.copy(0.5f), GlassWhite2)),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top shine
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(0.07f), Color.Transparent)),
                    RoundedCornerShape(16.dp)
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
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
    glowColor: Color = gradientColors[0],
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "scale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "sheen")
    val sheenPos by infiniteTransition.animateFloat(
        initialValue = -1.5f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(2800, delayMillis = 1400, easing = FastOutSlowInEasing)),
        label = "sheenPos"
    )

    Box(
        modifier = modifier
            .tiltOnTouch(6f)
            .scale(scale)
            .coloredShadow(glowColor, 20.dp, 28.dp, alpha = 0.55f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable { pressed = true; onClick() }
    ) {
        // Sheen sweep
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(0.14f), Color.Transparent),
                        start = Offset(sheenPos * 300f, 0f),
                        end = Offset(sheenPos * 300f + 200f, 200f)
                    )
                )
        )
        // Top shine highlight (3D glass effect)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(0.18f), Color.Transparent)),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        )
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .coloredShadow(Color.Black, 26.dp, 8.dp, alpha = 0.3f)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape)
                    .border(1.dp, Color.White.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
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
            .coloredShadow(color, 14.dp, 12.dp, alpha = 0.18f)
            .background(
                Brush.linearGradient(listOf(CardSurface, DarkSurface)),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(
                    Brush.verticalGradient(listOf(color, color.copy(0.2f))),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.18f), CircleShape)
                .border(1.dp, color.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

private fun drawEnhancedBackground(scope: DrawScope, t: Float) {
    with(scope) {
        val w = size.width
        val h = size.height
        val t2pi = t * 2 * PI.toFloat()

        // Main purple orb — larger, more visible
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x2E6C63FF), Color.Transparent),
                center = Offset(w * 0.2f + w * 0.15f * sin(t2pi), h * 0.25f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.2f + w * 0.15f * sin(t2pi), h * 0.25f)
        )
        // Cyan orb
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x2200F5FF), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.55f + h * 0.12f * cos(t2pi)),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.85f, h * 0.55f + h * 0.12f * cos(t2pi))
        )
        // Pink orb
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x22FF006E), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.85f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.5f, h * 0.85f)
        )
        // Small accent orb — moves independently
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x1800F5FF), Color.Transparent),
                center = Offset(w * 0.1f + w * 0.08f * cos(t2pi * 1.3f), h * 0.7f + h * 0.05f * sin(t2pi * 0.7f)),
                radius = w * 0.28f
            ),
            radius = w * 0.28f,
            center = Offset(w * 0.1f + w * 0.08f * cos(t2pi * 1.3f), h * 0.7f + h * 0.05f * sin(t2pi * 0.7f))
        )
        // Faint grid dots
        val dotSpacing = w / 10f
        val dotAlpha = 0.04f
        var col = 0
        while (col * dotSpacing <= w + dotSpacing) {
            var row = 0
            while (row * dotSpacing <= h + dotSpacing) {
                drawCircle(
                    color = Color.White.copy(alpha = dotAlpha),
                    radius = 1.5f,
                    center = Offset(col * dotSpacing, row * dotSpacing)
                )
                row++
            }
            col++
        }
    }
}
