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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(GlassWhite, CircleShape)) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Network Vault", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${uiState.networkCount} networks saved", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQuery,
            placeholder = { Text("Search networks...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextMuted) },
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
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.onCategorySelect(category) },
                    label = { Text(category, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = Color.White,
                        containerColor = GlassWhite,
                        labelColor = TextSecondary
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
                items(uiState.networks, key = { it.id }) { network ->
                    NetworkCard(
                        network = network,
                        context = context,
                        onToggleFavorite = { viewModel.toggleFavorite(network) },
                        onDelete = { viewModel.deleteNetwork(network) },
                        onConnect = { viewModel.connectToWifi(context, network) },
                        onGenerateQr = { onNavigateGenerate(network) }
                    )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(
                if (network.isFavorite) 1.5.dp else 1.dp,
                if (network.isFavorite) Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                else Brush.linearGradient(listOf(GlassWhite2, GlassWhite2)),
                RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // WiFi icon with signal indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.3f), NeonCyan.copy(alpha = 0.2f))),
                            CircleShape
                        ),
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
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(secDisplay, style = MaterialTheme.typography.labelSmall, color = secColor)
                        }
                        if (network.category != "General") {
                            Box(
                                modifier = Modifier
                                    .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
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
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = GlassWhite2, thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    // Password row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassWhite, RoundedCornerShape(12.dp))
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
                                onClick = {
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Password", network.password))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, null, tint = TextMuted, modifier = Modifier.size(16.dp))
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
                        OutlinedButton(
                            onClick = onConnect,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeonPurple),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.NetworkWifi, null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Connect", color = NeonPurple, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onGenerateQr,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.QrCode2, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("QR Code", color = NeonCyan, style = MaterialTheme.typography.labelMedium)
                        }
                        if (!showDeleteConfirm) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RedError.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, null, tint = RedError, modifier = Modifier.size(16.dp))
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
fun EmptyVaultState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            val infiniteTransition = rememberInfiniteTransition(label = "float")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -8f, targetValue = 8f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "float"
            )
            Icon(
                Icons.Rounded.WifiOff, null, tint = NeonPurple,
                modifier = Modifier.size(80.dp).offset(y = offsetY.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text("Vault is Empty", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan a WiFi QR code or generate one to save networks here",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
