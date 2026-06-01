package com.airkey.wifiqr.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.data.*
import com.airkey.wifiqr.ui.components.QrCodeGenerator
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.QrStyle
import com.airkey.wifiqr.viewmodel.WifiViewModel
import android.graphics.BitmapFactory
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream

@Composable
fun GeneratorScreen(
    viewModel: WifiViewModel,
    prefillNetwork: WifiNetwork? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val qrStyle by viewModel.qrStyle.collectAsState()

    var ssid by remember { mutableStateOf(prefillNetwork?.ssid ?: "") }
    var password by remember { mutableStateOf(prefillNetwork?.password ?: "") }
    var securityType by remember { mutableStateOf(prefillNetwork?.securityType ?: SecurityType.WPA.name) }
    var isHidden by remember { mutableStateOf(prefillNetwork?.isHidden ?: false) }
    var showPassword by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) }
    var shareToast by remember { mutableStateOf("") }
    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val network = remember(ssid, password, securityType, isHidden) {
        WifiNetwork(ssid = ssid, password = password, securityType = securityType, isHidden = isHidden)
    }
    val qrContent = network.toWifiQrString()
    val qrBitmap = remember(qrContent, qrStyle, logoBitmap) {
        if (ssid.isNotBlank()) QrCodeGenerator.generate(qrContent, 512, qrStyle, logoBitmap) else null
    }

    LaunchedEffect(shareToast) {
        if (shareToast.isNotEmpty()) {
            kotlinx.coroutines.delay(2000)
            shareToast = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(GlassWhite, CircleShape)) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "QR Generator",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Designer-level QR codes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            if (qrBitmap != null) {
                IconButton(
                    onClick = { shareQrCode(context, qrBitmap) },
                    modifier = Modifier.background(NeonPurple.copy(0.2f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Share, null, tint = NeonPurple)
                }
            }
        }

        // Tabs
        val tabs = listOf("Details", "Style", "Preview")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .background(GlassWhite, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { i, tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeTab == i)
                                Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                            else
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { activeTab = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (activeTab == i) Color.White else TextSecondary,
                        fontWeight = if (activeTab == i) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tab content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (activeTab) {
                0 -> DetailsTab(
                    ssid = ssid, onSsidChange = { ssid = it },
                    password = password, onPasswordChange = { password = it },
                    showPassword = showPassword, onTogglePassword = { showPassword = !showPassword },
                    securityType = securityType, onSecurityChange = { securityType = it },
                    isHidden = isHidden, onHiddenChange = { isHidden = it },
                    onSave = {
                        viewModel.saveNetwork(WifiNetwork(ssid = ssid, password = password, securityType = securityType, isHidden = isHidden))
                    },
                    canSave = ssid.isNotBlank()
                )
                1 -> StyleTab(
                    qrStyle = qrStyle,
                    onStyleChange = viewModel::updateQrStyle,
                    logoBitmap = logoBitmap,
                    onLogoBitmapChange = { logoBitmap = it }
                )
                2 -> PreviewTab(
                    qrBitmap = qrBitmap,
                    qrStyle = qrStyle,
                    ssid = ssid,
                    context = context,
                    onShare = { if (qrBitmap != null) shareQrCode(context, qrBitmap) },
                    onSaveToGallery = {
                        if (qrBitmap != null) {
                            saveQrToGallery(context, qrBitmap)
                            shareToast = "Saved to gallery!"
                        }
                    },
                    onToast = { shareToast = it }
                )
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun DetailsTab(
    ssid: String, onSsidChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    showPassword: Boolean, onTogglePassword: () -> Unit,
    securityType: String, onSecurityChange: (String) -> Unit,
    isHidden: Boolean, onHiddenChange: (Boolean) -> Unit,
    onSave: () -> Unit, canSave: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader("Network Details", Icons.Rounded.Wifi)

        OutlinedTextField(
            value = ssid,
            onValueChange = onSsidChange,
            label = { Text("WiFi Name (SSID)") },
            leadingIcon = { Icon(Icons.Rounded.Wifi, null, tint = NeonPurple) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = airKeyTextFieldColors(),
            singleLine = true,
            supportingText = { Text("The network name visible to devices", color = TextMuted, style = MaterialTheme.typography.labelSmall) }
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Key, null, tint = NeonCyan) },
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        null, tint = TextMuted
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = airKeyTextFieldColors(),
            singleLine = true,
            supportingText = {
                if (password.isNotEmpty()) {
                    val strength = passwordStrength(password)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(strength.first, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Password strength: ${strength.second}", color = strength.first, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        )

        Text("Security Type", style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecurityType.values().forEach { sec ->
                val isSelected = securityType == sec.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) NeonPurple.copy(0.15f) else GlassWhite)
                        .border(
                            1.dp,
                            if (isSelected) NeonPurple.copy(0.6f) else GlassWhite2,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSecurityChange(sec.name) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSecurityChange(sec.name) },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonPurple)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            sec.display,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            when (sec) {
                                SecurityType.WPA -> "Recommended — most compatible"
                                SecurityType.WPA3 -> "Latest standard — most secure"
                                SecurityType.WEP -> "Legacy — use only if required"
                                SecurityType.OPEN -> "No password — public network"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    if (sec == SecurityType.WPA3) {
                        Icon(Icons.Rounded.Star, null, tint = GreenSuccess, modifier = Modifier.size(16.dp))
                    }
                    if (sec == SecurityType.WEP) {
                        Icon(Icons.Rounded.Warning, null, tint = OrangeWarn, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Hidden network toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassWhite, RoundedCornerShape(16.dp))
                .border(1.dp, GlassWhite2, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.VisibilityOff, null, tint = NeonPink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hidden Network", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text("SSID is not broadcast publicly", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Switch(
                checked = isHidden,
                onCheckedChange = onHiddenChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonPurple
                )
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
            enabled = canSave
        ) {
            Icon(Icons.Rounded.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Save to Vault", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StyleTab(
    qrStyle: QrStyle,
    onStyleChange: (QrStyle) -> Unit,
    logoBitmap: Bitmap?,
    onLogoBitmapChange: (Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bmp = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(
                        android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                }
            } catch (e: Exception) { null }
            onLogoBitmapChange(bmp)
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader("Color Theme", Icons.Rounded.Palette)
        val colorPresets = listOf(
            Triple("Neon Violet", 0xFF6C63FF, 0xFF00F5FF),
            Triple("Cyber Pink", 0xFFFF006E, 0xFF6C63FF),
            Triple("Matrix Green", 0xFF00E676, 0xFF00BCD4),
            Triple("Solar Flame", 0xFFFFAB40, 0xFFFF5252),
            Triple("Ocean Blue", 0xFF0288D1, 0xFF00E5FF),
            Triple("Gold Rush", 0xFFFFD700, 0xFFFF8C00),
            Triple("Midnight", 0xFF7B1FA2, 0xFF3F51B5),
            Triple("Rose Gold", 0xFFFF6090, 0xFFFFB347),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(colorPresets) { (name, fg, accent) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onStyleChange(qrStyle.copy(foregroundColor = fg, accentColor = accent, patternIndex = 1)) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(fg.toInt()), Color(accent.toInt()))))
                            .border(
                                if (qrStyle.foregroundColor == fg) 2.dp else 0.dp,
                                Color.White, CircleShape
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name.split(" ")[0],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (qrStyle.foregroundColor == fg) NeonCyan else TextMuted
                    )
                }
            }
        }

        SectionHeader("Color Pattern", Icons.Rounded.AutoAwesome)
        val patterns = listOf(
            Pair("Solid", Icons.Rounded.Circle),
            Pair("Diagonal Gradient", Icons.Rounded.NorthEast),
            Pair("Radial Gradient", Icons.Rounded.RadioButtonChecked),
            Pair("Horizontal", Icons.Rounded.CompareArrows),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(patterns) { i, (label, icon) ->
                PatternChip(
                    label = label,
                    icon = icon,
                    selected = qrStyle.patternIndex == i,
                    onClick = { onStyleChange(qrStyle.copy(patternIndex = i)) }
                )
            }
        }

        SectionHeader("Dot Shape", Icons.Rounded.BubbleChart)
        val shapes = listOf(
            Pair("Rounded", Icons.Rounded.RoundedCorner),
            Pair("Circle", Icons.Rounded.Circle),
            Pair("Diamond", Icons.Rounded.ChangeHistory),
            Pair("Star", Icons.Rounded.Star),
            Pair("Heart", Icons.Rounded.Favorite),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(shapes) { i, (label, icon) ->
                PatternChip(
                    label = label,
                    icon = icon,
                    selected = qrStyle.dotShape == i,
                    onClick = { onStyleChange(qrStyle.copy(dotShape = i)) },
                    color = NeonCyan
                )
            }
        }

        SectionHeader("Frame Style", Icons.Rounded.BorderOuter)
        val frames = listOf(
            Pair("None", Icons.Rounded.CheckBoxOutlineBlank),
            Pair("Glass Border", Icons.Rounded.Window),
            Pair("Neon Glow", Icons.Rounded.Flare),
            Pair("Cyberpunk", Icons.Rounded.ViewCompact),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(frames) { i, (label, icon) ->
                PatternChip(
                    label = label,
                    icon = icon,
                    selected = qrStyle.frameStyle == i,
                    onClick = { onStyleChange(qrStyle.copy(frameStyle = i)) },
                    color = NeonPink
                )
            }
        }

        SectionHeader("Center Logo", Icons.Rounded.Stars)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassWhite, RoundedCornerShape(14.dp))
                .border(1.dp, if (qrStyle.showLogo) NeonPurple.copy(0.5f) else GlassWhite2, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Stars, null, tint = NeonPurple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Logo Center", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Branded logo inside the QR code", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Switch(
                checked = qrStyle.showLogo,
                onCheckedChange = { onStyleChange(qrStyle.copy(showLogo = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonPurple)
            )
        }
        AnimatedVisibility(visible = qrStyle.showLogo, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = qrStyle.logoText,
                    onValueChange = { onStyleChange(qrStyle.copy(logoText = it.take(3))) },
                    label = { Text("Logo Text (max 3 chars)") },
                    leadingIcon = { Icon(Icons.Rounded.TextFields, null, tint = NeonPurple) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = airKeyTextFieldColors(),
                    singleLine = true,
                    supportingText = { Text("Text shows when no image logo is selected", color = TextMuted, style = MaterialTheme.typography.labelSmall) }
                )
                if (logoBitmap != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassWhite, RoundedCornerShape(14.dp))
                            .border(1.dp, GreenSuccess.copy(0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GlassWhite2),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Logo Image Set", style = MaterialTheme.typography.labelMedium, color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                            Text("Replaces text in QR center", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        IconButton(onClick = { onLogoBitmapChange(null) }) {
                            Icon(Icons.Rounded.Close, null, tint = Color(0xFFFF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (logoBitmap != null) NeonCyan.copy(0.6f) else NeonPurple.copy(0.5f))
                ) {
                    Icon(
                        Icons.Rounded.Image, null,
                        tint = if (logoBitmap != null) NeonCyan else NeonPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (logoBitmap != null) "Change Logo Image" else "Import Logo Image",
                        color = if (logoBitmap != null) NeonCyan else NeonPurple
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewTab(
    qrBitmap: Bitmap?,
    qrStyle: QrStyle,
    ssid: String,
    context: Context,
    onShare: () -> Unit,
    onSaveToGallery: () -> Unit,
    onToast: (String) -> Unit
) {
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            if (qrBitmap != null) {
                val ok = saveQrToFolder(context, qrBitmap, it)
                onToast(if (ok) "Saved to selected folder!" else "Save failed — try again")
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (qrBitmap != null) {
            val qrColor = Color(qrStyle.foregroundColor.toInt())
            val accentColor = Color(qrStyle.accentColor.toInt())

            val infiniteTransition = rememberInfiniteTransition(label = "qrAnim")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f, targetValue = 0.85f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "glow"
            )
            val ringRotation by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
                label = "ring"
            )
            val ringRotation2 by infiniteTransition.animateFloat(
                initialValue = 360f, targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
                label = "ring2"
            )
            val scanLine by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
                label = "scan"
            )

            Box(contentAlignment = Alignment.Center) {
                // Outer diffuse glow
                Box(
                    modifier = Modifier
                        .size(340.dp)
                        .background(
                            Brush.radialGradient(listOf(qrColor.copy(glowAlpha * 0.18f), Color.Transparent)),
                            CircleShape
                        )
                )
                // Outer slow counter-rotating ring
                Box(
                    modifier = Modifier
                        .size(308.dp)
                        .graphicsLayer { rotationZ = ringRotation2 }
                        .border(
                            1.dp,
                            Brush.sweepGradient(listOf(Color.Transparent, accentColor.copy(0.5f), Color.Transparent, qrColor.copy(0.3f), Color.Transparent)),
                            RoundedCornerShape(36.dp)
                        )
                )
                // Inner fast rotating ring
                Box(
                    modifier = Modifier
                        .size(284.dp)
                        .graphicsLayer { rotationZ = ringRotation }
                        .border(
                            1.5.dp,
                            Brush.sweepGradient(listOf(Color.Transparent, qrColor.copy(0.8f), accentColor.copy(0.6f), Color.Transparent)),
                            RoundedCornerShape(30.dp)
                        )
                )
                // Tight glow halo
                Box(
                    modifier = Modifier
                        .size(272.dp)
                        .background(
                            Brush.radialGradient(listOf(qrColor.copy(glowAlpha * 0.22f), Color.Transparent)),
                            CircleShape
                        )
                )
                // QR card
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .coloredShadow(qrColor, 24.dp, 32.dp, alpha = 0.65f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(qrStyle.backgroundColor.toInt()))
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(qrColor.copy(0.9f), accentColor.copy(0.7f))),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "WiFi QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                    // Animated scan line overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = (scanLine * 244).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, qrColor.copy(0.7f), accentColor.copy(0.7f), Color.Transparent)
                                    )
                                )
                        )
                    }
                    // Top shine
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(listOf(Color.White.copy(0.12f), Color.Transparent)),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                    )
                }
            }

            // Network name label
            if (ssid.isNotBlank()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .coloredShadow(NeonPurple, 14.dp, 12.dp, alpha = 0.2f)
                        .background(
                            Brush.linearGradient(listOf(CardSurface, DarkSurface)),
                            RoundedCornerShape(14.dp)
                        )
                        .border(1.dp, GlassWhite2, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        ssid,
                        style = MaterialTheme.typography.titleMedium.copy(
                            brush = Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    Text("Scan to connect instantly", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }

            // Info box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .coloredShadow(GreenSuccess, 14.dp, 10.dp, alpha = 0.2f)
                    .background(GreenSuccess.copy(0.08f), RoundedCornerShape(14.dp))
                    .border(1.dp, GreenSuccess.copy(0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "High error correction — scannable even with logo overlay",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenSuccess.copy(0.9f)
                    )
                }
            }

            // Share / Save actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .coloredShadow(NeonPurple, 14.dp, 16.dp, alpha = 0.4f)
                    ) {
                        OutlinedButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, NeonPurple),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Rounded.Share, null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share QR", color = NeonPurple, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .coloredShadow(NeonCyan, 14.dp, 16.dp, alpha = 0.4f)
                    ) {
                        Button(
                            onClick = onSaveToGallery,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Rounded.Download, null, tint = DeepBlack, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save PNG", color = DeepBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .coloredShadow(NeonPink, 14.dp, 16.dp, alpha = 0.35f)
                ) {
                    Button(
                        onClick = { if (qrBitmap != null) folderPickerLauncher.launch(null) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = qrBitmap != null
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save to Folder", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // QR format info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .coloredShadow(NeonPurple, 16.dp, 10.dp, alpha = 0.15f)
                    .background(
                        Brush.linearGradient(listOf(CardSurface, DarkSurface)),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, GlassWhite2, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Compatible with", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompatBadge("Android Camera", Icons.Rounded.PhoneAndroid)
                    CompatBadge("iPhone Camera", Icons.Rounded.PhoneIphone)
                    CompatBadge("Google Lens", Icons.Rounded.Search)
                }
            }
        } else {
            Spacer(Modifier.height(40.dp))
            val infiniteTransition = rememberInfiniteTransition(label = "emptyQr")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f, targetValue = 0.5f,
                animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulse"
            )
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            Brush.radialGradient(listOf(NeonPurple.copy(pulseAlpha * 0.2f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .coloredShadow(NeonPurple, 24.dp, 20.dp, alpha = 0.2f)
                        .background(
                            Brush.linearGradient(listOf(CardSurface, DarkSurface)),
                            RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, Brush.linearGradient(listOf(NeonPurple.copy(0.3f), GlassWhite2)), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.QrCode2, null, tint = NeonPurple.copy(pulseAlpha + 0.3f), modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Fill in Details tab\nto preview your QR code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.CompatBadge(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(GlassWhite, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = GreenSuccess, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NeonPurple, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PatternChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = NeonPurple
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) color.copy(0.2f) else GlassWhite)
                .border(1.5.dp, if (selected) color else GlassWhite2, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (selected) color else TextMuted, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else TextMuted,
            maxLines = 1
        )
    }
}

@Composable
fun airKeyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonPurple,
    unfocusedBorderColor = GlassWhite2,
    focusedLabelColor = NeonPurple,
    unfocusedLabelColor = TextMuted,
    cursorColor = NeonPurple,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = GlassWhite,
    unfocusedContainerColor = Color.Transparent
)

fun passwordStrength(password: String): Pair<Color, String> {
    if (password.length < 6) return Pair(RedError, "Very weak")
    if (password.length < 8) return Pair(OrangeWarn, "Weak")
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val score = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
    return when {
        password.length >= 12 && score >= 3 -> Pair(GreenSuccess, "Strong")
        password.length >= 8 && score >= 2 -> Pair(NeonCyan, "Good")
        else -> Pair(OrangeWarn, "Moderate")
    }
}

fun shareQrCode(context: Context, bitmap: Bitmap) {
    try {
        val file = File(context.cacheDir, "airkey_qr_${System.currentTimeMillis()}.png")
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
            Intent.createChooser(intent, "Share WiFi QR Code")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun saveQrToGallery(context: Context, bitmap: Bitmap) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "AirKey_QR_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AirKey")
            }
            val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) } }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AirKey")
            dir.mkdirs()
            val file = File(dir, "AirKey_QR_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun saveQrToFolder(context: Context, bitmap: Bitmap, folderUri: Uri): Boolean {
    return try {
        val fileName = "AirKey_QR_${System.currentTimeMillis()}.png"
        val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocId)
        val fileUri = DocumentsContract.createDocument(
            context.contentResolver, parentDocUri, "image/png", fileName
        )
        fileUri?.let { uri ->
            context.contentResolver.openOutputStream(uri)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
        }
        fileUri != null
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
