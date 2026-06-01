package com.airkey.wifiqr.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.data.*
import com.airkey.wifiqr.ui.components.QrCodeGenerator
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.QrStyle
import com.airkey.wifiqr.viewmodel.WifiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    var activeTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .systemBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DarkSurface, DeepBlack)))
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

        // ── Tab switcher ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(GlassWhite, RoundedCornerShape(18.dp))
                .border(1.dp, GlassWhite2, RoundedCornerShape(18.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple("Networks", Icons.Rounded.Wifi, NeonPurple),
                Triple("QR Codes", Icons.Rounded.QrCode2, NeonCyan)
            ).forEachIndexed { i, (label, icon, tintColor) ->
                val selected = activeTab == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (selected) Modifier
                                .coloredShadow(tintColor, 16.dp, 8.dp, alpha = 0.4f)
                                .background(
                                    Brush.linearGradient(listOf(NeonPurple.copy(0.85f), NeonCyan.copy(0.85f))),
                                    RoundedCornerShape(14.dp)
                                )
                            else Modifier.background(Color.Transparent, RoundedCornerShape(14.dp))
                        )
                        .clickable { activeTab = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            icon, null,
                            tint = if (selected) Color.White else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else TextMuted
                        )
                    }
                }
            }
        }

        // ── Search bar ───────────────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQuery,
            placeholder = { Text("Search networks…", color = TextMuted) },
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
                .padding(horizontal = 16.dp, vertical = 2.dp),
            shape = RoundedCornerShape(16.dp),
            colors = airKeyTextFieldColors(),
            singleLine = true
        )

        // ── Category chips ───────────────────────────────────────────────────
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.categories) { category ->
                val chipColor = when (category) {
                    "All" -> NeonPurple; "Home" -> NeonPurple; "Work" -> NeonCyan
                    "Travel" -> OrangeWarn; "Public" -> NeonPink; "Guest" -> GreenSuccess
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
                        enabled = true, selected = isSelected,
                        selectedBorderColor = chipColor, borderColor = GlassWhite2
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // ── Content ──────────────────────────────────────────────────────────
        if (uiState.networks.isEmpty()) {
            EmptyVaultState()
        } else {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    slideInHorizontally { dir * it } + fadeIn() togetherWith
                            slideOutHorizontally { -dir * it } + fadeOut()
                },
                label = "tab"
            ) { tab ->
                if (tab == 0) {
                    NetworksTab(
                        networks = uiState.networks,
                        context = context,
                        viewModel = viewModel,
                        onNavigateGenerate = onNavigateGenerate
                    )
                } else {
                    QrCodesTab(
                        networks = uiState.networks,
                        context = context,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Networks Tab
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun NetworksTab(
    networks: List<WifiNetwork>,
    context: Context,
    viewModel: WifiViewModel,
    onNavigateGenerate: (WifiNetwork) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(networks, key = { _, n -> n.id }) { index, network ->
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
                    onConnectInstantly = { viewModel.connectInstantly(context, network) },
                    onGenerateQr = { onNavigateGenerate(network) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// QR Codes Tab
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun QrCodesTab(
    networks: List<WifiNetwork>,
    context: Context,
    viewModel: WifiViewModel
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(networks, key = { _, n -> n.id }) { index, network ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index.coerceAtMost(6) * 70L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                ) + fadeIn(tween(350))
            ) {
                QrNetworkCard(
                    network = network,
                    context = context,
                    onToggleFavorite = { viewModel.toggleFavorite(network) },
                    onDelete = { viewModel.deleteNetwork(network) },
                    onConnectInstantly = { viewModel.connectInstantly(context, network) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// QR Network Card (QR-focused, shows everything)
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun QrNetworkCard(
    network: WifiNetwork,
    context: Context,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onConnectInstantly: () -> Unit
) {
    var qrBitmap by remember(network.id) { mutableStateOf<Bitmap?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var savedToGallery by remember { mutableStateOf(false) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LaunchedEffect(network.id, network.ssid, network.password, network.securityType) {
        qrBitmap = withContext(Dispatchers.Default) {
            QrCodeGenerator.generate(network.toWifiQrString(), 360, QrStyle())
        }
    }

    val secDisplay = try { SecurityType.valueOf(network.securityType).display } catch (_: Exception) { network.securityType }
    val secColor = when (network.securityType) {
        SecurityType.OPEN.name -> RedError
        SecurityType.WEP.name -> OrangeWarn
        SecurityType.WPA3.name -> GreenSuccess
        else -> NeonCyan
    }
    val categoryColor = when (network.category) {
        "Home" -> NeonPurple; "Work" -> NeonCyan; "Travel" -> OrangeWarn
        "Public" -> NeonPink; "Guest" -> GreenSuccess; else -> TextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .coloredShadow(
                if (network.isFavorite) NeonPurple else NeonCyan,
                24.dp, 20.dp,
                alpha = if (network.isFavorite) 0.45f else 0.2f
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(CardSurface, DarkSurface.copy(0.97f))))
            .border(
                if (network.isFavorite) 1.5.dp else 1.dp,
                if (network.isFavorite)
                    Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                else
                    Brush.linearGradient(listOf(GlassWhite2, GlassWhite)),
                RoundedCornerShape(24.dp)
            )
    ) {
        // Top shine
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(0.06f), Color.Transparent)),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        )

        Column(modifier = Modifier.padding(16.dp)) {

            // ── Top row: SSID + favourite ──────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .coloredShadow(NeonPurple, 21.dp, 8.dp, alpha = 0.3f)
                        .background(
                            Brush.linearGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.2f))),
                            CircleShape
                        )
                        .border(1.dp, NeonPurple.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Wifi, null, tint = NeonPurple, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        network.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(secColor.copy(0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, secColor.copy(0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(secDisplay, style = MaterialTheme.typography.labelSmall, color = secColor, fontWeight = FontWeight.SemiBold)
                        }
                        if (network.category != "General") {
                            Box(
                                modifier = Modifier
                                    .background(categoryColor.copy(0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, categoryColor.copy(0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(network.category, style = MaterialTheme.typography.labelSmall, color = categoryColor)
                            }
                        }
                        if (network.isHidden) {
                            Box(
                                modifier = Modifier
                                    .background(TextMuted.copy(0.12f), RoundedCornerShape(6.dp))
                                    .border(1.dp, TextMuted.copy(0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Hidden", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
            }

            Spacer(Modifier.height(16.dp))

            // ── QR Code image ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .coloredShadow(NeonPurple, 32.dp, 24.dp, alpha = 0.35f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(DarkSurface, DeepBlack)))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(NeonPurple.copy(0.5f), NeonCyan.copy(0.5f))),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR code for ${network.ssid}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        color = NeonPurple,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Divider ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.linearGradient(listOf(Color.Transparent, GlassWhite2, Color.Transparent)))
            )

            Spacer(Modifier.height(14.dp))

            // ── Password row ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(GlassWhite, Color.Transparent)), RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan.copy(0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Key, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Password", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        if (showPassword) network.password.ifEmpty { "(No password)" }
                        else if (network.password.isEmpty()) "(No password)"
                        else "●".repeat(network.password.length.coerceAtMost(20)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (network.password.isEmpty()) TextMuted else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (network.password.isNotEmpty()) {
                    IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            null, tint = TextMuted, modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Password", network.password))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                buildString {
                    append("Saved ${dateFormat.format(Date(network.savedAt))}")
                    network.lastConnected?.let { append("  ·  Last connected ${dateFormat.format(Date(it))}") }
                    if (network.notes.isNotEmpty()) append("  ·  ${network.notes}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            // ── Action buttons row 1: Share + Save ─────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { qrBitmap?.let { savedQrShareBitmap(context, it) } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple.copy(0.18f),
                        contentColor = NeonPurple
                    ),
                    border = BorderStroke(1.dp, NeonPurple.copy(0.55f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    enabled = qrBitmap != null
                ) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Share QR", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = {
                        qrBitmap?.let {
                            savedQrSaveGallery(context, it)
                            savedToGallery = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (savedToGallery) GreenSuccess.copy(0.18f) else NeonCyan.copy(0.18f),
                        contentColor = if (savedToGallery) GreenSuccess else NeonCyan
                    ),
                    border = BorderStroke(1.dp, if (savedToGallery) GreenSuccess.copy(0.55f) else NeonCyan.copy(0.55f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    enabled = qrBitmap != null
                ) {
                    Icon(
                        if (savedToGallery) Icons.Rounded.Check else Icons.Rounded.SaveAlt,
                        null, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(if (savedToGallery) "Saved!" else "Save QR", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Action button row 2: Connect Instantly + Delete ────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onConnectInstantly,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSuccess.copy(0.18f),
                        contentColor = GreenSuccess
                    ),
                    border = BorderStroke(1.dp, GreenSuccess.copy(0.55f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Connect Instantly", style = MaterialTheme.typography.labelMedium)
                }

                if (!showDeleteConfirm) {
                    Button(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedError.copy(0.12f),
                            contentColor = RedError
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, RedError.copy(0.4f))
                    ) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = { showDeleteConfirm = false; onDelete() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedError),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("Confirm?", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Network Card (compact list view with Connect Instantly)
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun NetworkCard(
    network: WifiNetwork,
    context: Context,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onConnectInstantly: () -> Unit,
    onGenerateQr: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val secDisplay = try { SecurityType.valueOf(network.securityType).display } catch (_: Exception) { network.securityType }
    val secColor = when (network.securityType) {
        SecurityType.OPEN.name -> RedError; SecurityType.WEP.name -> OrangeWarn
        SecurityType.WPA3.name -> GreenSuccess; else -> NeonCyan
    }
    val categoryColor = when (network.category) {
        "Home" -> NeonPurple; "Work" -> NeonCyan; "Travel" -> OrangeWarn
        "Public" -> NeonPink; "Guest" -> GreenSuccess; else -> TextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .coloredShadow(
                if (network.isFavorite) NeonPurple else secColor,
                20.dp, 18.dp,
                alpha = if (network.isFavorite) 0.45f else 0.25f
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(CardSurface, DarkSurface.copy(0.95f))))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .coloredShadow(NeonPurple, 23.dp, 10.dp, alpha = 0.3f)
                        .background(
                            Brush.linearGradient(listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.2f))),
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(secColor.copy(0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, secColor.copy(0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(secDisplay, style = MaterialTheme.typography.labelSmall, color = secColor, fontWeight = FontWeight.SemiBold)
                        }
                        if (network.category != "General") {
                            Box(
                                modifier = Modifier
                                    .background(categoryColor.copy(0.15f), RoundedCornerShape(6.dp))
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

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Brush.linearGradient(listOf(Color.Transparent, GlassWhite2, Color.Transparent)))
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(GlassWhite, Color.Transparent)), RoundedCornerShape(12.dp))
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onConnectInstantly,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenSuccess.copy(0.18f),
                                contentColor = GreenSuccess
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, GreenSuccess.copy(0.55f))
                        ) {
                            Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Connect", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onGenerateQr,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan.copy(0.18f),
                                contentColor = NeonCyan
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(0.55f))
                        ) {
                            Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.size(15.dp))
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

// ────────────────────────────────────────────────────────────────────────────
// Empty state
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun EmptyVaultState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        .background(Brush.radialGradient(listOf(NeonPurple.copy(glowAlpha * 0.3f), Color.Transparent)), CircleShape)
                )
                Icon(Icons.Rounded.WifiOff, null, tint = NeonPurple, modifier = Modifier.size(72.dp).offset(y = offsetY.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Vault is Empty", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan a WiFi QR code or generate one\nto save networks here",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Utility: Share + Save QR
// ────────────────────────────────────────────────────────────────────────────
private fun savedQrShareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val file = File(context.cacheDir, "airkey_share_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Scan to connect to WiFi instantly! Made with AirKey")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share WiFi QR Code").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) { e.printStackTrace() }
}

private fun savedQrSaveGallery(context: Context, bitmap: Bitmap) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "AirKey_QR_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AirKey")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) } }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AirKey")
            dir.mkdirs()
            val file = File(dir, "AirKey_QR_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    } catch (e: Exception) { e.printStackTrace() }
}
