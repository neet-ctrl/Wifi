package com.airkey.wifiqr.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.data.*
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavedNetworksScreen(
    viewModel: WifiViewModel,
    onBack: () -> Unit,
    onNavigateGenerate: (WifiNetwork) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .systemBarsPadding()
    ) {
        // Header with gradient accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(DarkSurface, DeepBlack)),
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .coloredShadow(NeonPurple, 22.dp, 12.dp, alpha = 0.35f)
                        .background(
                            Brush.linearGradient(listOf(NeonPurple.copy(0.25f), NeonCyan.copy(0.15f))),
                            CircleShape
                        )
                        .border(1.dp, NeonPurple.copy(0.4f), CircleShape)
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Network Vault",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${uiState.networkCount} networks saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                // Network count badge
                if (uiState.networkCount > 0) {
                    Box(
                        modifier = Modifier
                            .coloredShadow(NeonCyan, 14.dp, 10.dp, alpha = 0.4f)
                            .background(
                                Brush.linearGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.3f))),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${uiState.networkCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQuery,
            placeholder = { Text("Search networks...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = NeonPurple) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQuery("") }) {
                        Icon(Icons.Rounded.Clear, null, tint = TextMuted)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = airKeyTextFieldColors(),
            singleLine = true
        )

        // Category chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.categories) { category ->
                val chipColor = when (category) {
                    "All" -> NeonPurple
                    "Home" -> NeonPurple
                    "Work" -> NeonCyan
                    "Travel" -> OrangeWarn
                    "Public" -> NeonPink
                    "Guest" -> GreenSuccess
                    else -> TextMuted
                }
                val isSelected = uiState.selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onCategorySelect(category) },
                    label = { Text(category, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(0.2f),
                        selectedLabelColor = chipColor,
                        containerColor = GlassWhite,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = chipColor,
                        borderColor = GlassWhite2
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Network list
        if (uiState.networks.isEmpty()) {
            EmptyVaultState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(uiState.networks, key = { _, n -> n.id }) { index, network ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index.coerceAtMost(6) * 55L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                        ) + fadeIn(tween(300))
                    ) {
                        NetworkCard(
                            network = network,
                            context = context,
                            onToggleFavorite = { viewModel.toggleFavorite(network) },
                            onDelete = { viewModel.deleteNetwork(network) },
                            onConnect = { viewModel.connectToWifi(context, network) },
                            onGenerateQr = { onNavigateGenerate(network) }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun NetworkCard(
    network: WifiNetwork,
    context: Context,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onConnect: () -> Unit,
    onGenerateQr: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val secDisplay = try { SecurityType.valueOf(network.securityType).display } catch (e: Exception) { network.securityType }
    val secColor = when (network.securityType) {
        SecurityType.OPEN.name -> RedError
        SecurityType.WEP.name -> OrangeWarn
        SecurityType.WPA3.name -> GreenSuccess
        else -> NeonCyan
    }
    val categoryColor = when (network.category) {
        "Home" -> NeonPurple
        "Work" -> NeonCyan
        "Travel" -> OrangeWarn
        "Public" -> NeonPink
        "Guest" -> GreenSuccess
        else -> TextMuted
    }
    val shadowColor = if (network.isFavorite) NeonPurple else secColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .coloredShadow(shadowColor, 20.dp, 18.dp, alpha = if (network.isFavorite) 0.45f else 0.25f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(CardSurface, DarkSurface.copy(0.95f)),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.MAX_VALUE)
                )
            )
            .border(
                if (network.isFavorite) 1.5.dp else 1.dp,
                if (network.isFavorite)
                    Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                else
                    Brush.linearGradient(listOf(GlassWhite2, GlassWhite)),
                RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        // Top shine
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(0.05f), Color.Transparent)),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        )
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .coloredShadow(NeonPurple, 23.dp, 10.dp, alpha = 0.3f)
                        .background(
                            Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.3f), NeonCyan.copy(alpha = 0.2f))),
                            CircleShape
                        )
                        .border(1.dp, NeonPurple.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Wifi, null, tint = NeonPurple, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        network.ssid,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(secColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, secColor.copy(0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(secDisplay, style = MaterialTheme.typography.labelSmall, color = secColor, fontWeight = FontWeight.SemiBold)
                        }
                        if (network.category != "General") {
                            Box(
                                modifier = Modifier
                                    .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, categoryColor.copy(0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(network.category, style = MaterialTheme.typography.labelSmall, color = categoryColor)
                            }
                        }
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (network.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        null,
                        tint = if (network.isFavorite) NeonPink else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = TextMuted, modifier = Modifier.size(20.dp)
                )
            }

            // Expanded content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        color = Brush.linearGradient(listOf(Color.Transparent, GlassWhite2, Color.Transparent)),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(14.dp))

                    // Password row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(GlassWhite, Color.Transparent)),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, NeonCyan.copy(0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Key, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Password", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                if (showPassword) network.password.ifEmpty { "(No password)" }
                                else "●".repeat(network.password.length.coerceAtMost(14)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(32.dp)) {
                            Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                        if (network.password.isNotEmpty()) {
                            IconButton(
                                onClick = { clipboardManager.setPrimaryClip(ClipData.newPlainText("Password", network.password)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Saved ${dateFormat.format(Date(network.savedAt))}${network.lastConnected?.let { " · Last connected ${dateFormat.format(Date(it))}" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted
                    )
                    Spacer(Modifier.height(14.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onConnect,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple.copy(0.2f),
                                contentColor = NeonPurple
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, NeonPurple.copy(0.6f))
                        ) {
                            Icon(Icons.Rounded.NetworkWifi, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Connect", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onGenerateQr,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan.copy(0.2f),
                                contentColor = NeonCyan
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(0.6f))
                        ) {
                            Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("QR Code", style = MaterialTheme.typography.labelMedium)
                        }
                        if (!showDeleteConfirm) {
                            Button(
                                onClick = { showDeleteConfirm = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedError.copy(0.1f),
                                    contentColor = RedError
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                border = BorderStroke(1.dp, RedError.copy(0.4f))
                            ) {
                                Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                onClick = { showDeleteConfirm = false; onDelete() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedError),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("Confirm?", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider(color: Brush, thickness: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

@Composable
fun EmptyVaultState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            val infiniteTransition = rememberInfiniteTransition(label = "float")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -10f, targetValue = 10f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "float"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 0.7f,
                animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "glow"
            )
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(y = offsetY.dp)
                        .background(
                            Brush.radialGradient(listOf(NeonPurple.copy(glowAlpha * 0.3f), Color.Transparent)),
                            CircleShape
                        )
                )
                Icon(
                    Icons.Rounded.WifiOff, null, tint = NeonPurple,
                    modifier = Modifier.size(72.dp).offset(y = offsetY.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Vault is Empty",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan a WiFi QR code or generate one\nto save networks here",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
