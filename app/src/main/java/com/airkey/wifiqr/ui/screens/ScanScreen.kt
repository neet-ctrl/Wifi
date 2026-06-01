package com.airkey.wifiqr.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airkey.wifiqr.data.*
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    viewModel: WifiViewModel,
    onBack: () -> Unit,
    onNavigateGenerate: (WifiNetwork) -> Unit
) {
    val scannedResult by viewModel.scannedResult.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    var hasScanned by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(scannedResult) {
        if (scannedResult != null && !hasScanned) {
            hasScanned = true
            torchEnabled = false
            vibrate(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        when {
            cameraPermission.status.isGranted -> {
                if (scannedResult == null) {
                    CameraPreviewSection(
                        torchEnabled = torchEnabled,
                        onQrDetected = { raw ->
                            if (!hasScanned) viewModel.onQrScanned(raw)
                        }
                    )
                    ScanOverlay(
                        onBack = onBack,
                        torchEnabled = torchEnabled,
                        onTorchToggle = { torchEnabled = !torchEnabled }
                    )
                } else {
                    ScannedResultPanel(
                        result = scannedResult!!,
                        context = context,
                        viewModel = viewModel,
                        onScanAgain = {
                            hasScanned = false
                            viewModel.clearScannedResult()
                        },
                        onNavigateGenerate = onNavigateGenerate,
                        onBack = onBack
                    )
                }
            }
            else -> {
                PermissionScreen(
                    onRequest = { cameraPermission.launchPermissionRequest() },
                    onBack = onBack
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewSection(torchEnabled: Boolean, onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var lastScan by remember { mutableLongStateOf(0L) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(torchEnabled) {
        cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val now = System.currentTimeMillis()
                    if (now - lastScan > 800) {
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { raw ->
                                        lastScan = now
                                        onQrDetected(raw)
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else imageProxy.close()
                    } else imageProxy.close()
                }
            }

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
            cameraControl = camera.cameraControl
            cameraControl?.enableTorch(torchEnabled)
        } catch (e: Exception) {
            Log.e("ScanScreen", "Camera bind failed", e)
        }
    }
}

@Composable
fun ScanOverlay(onBack: () -> Unit, torchEnabled: Boolean, onTorchToggle: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLine by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "line"
    )
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    val scannerSize = 270

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
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
                    .background(GlassWhite, CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Scan WiFi QR Code",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Works with any phone or app",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = onTorchToggle,
                modifier = Modifier
                    .background(
                        if (torchEnabled) NeonCyan.copy(alpha = 0.25f) else GlassWhite,
                        CircleShape
                    )
                    .size(44.dp)
            ) {
                Icon(
                    if (torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                    contentDescription = if (torchEnabled) "Turn off torch" else "Turn on torch",
                    tint = if (torchEnabled) NeonCyan else Color.White
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(scannerSize.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
            Box(
                modifier = Modifier.size(scannerSize.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    val len = 38.dp.toPx()
                    val color = android.graphics.Color.argb(
                        (borderAlpha * 255).toInt(), 108, 99, 255
                    )
                    val accentColor = android.graphics.Color.argb(
                        (borderAlpha * 255).toInt(), 0, 245, 255
                    )
                    val paint = android.graphics.Paint().apply {
                        this.color = color
                        this.strokeWidth = stroke
                        this.style = android.graphics.Paint.Style.STROKE
                        this.isAntiAlias = true
                        setShadowLayer(10f, 0f, 0f, color)
                    }
                    val w = size.width
                    val h = size.height
                    drawIntoCanvas { canvas ->
                        // TL
                        canvas.nativeCanvas.drawLine(0f, 0f, len, 0f, paint)
                        canvas.nativeCanvas.drawLine(0f, 0f, 0f, len, paint)
                        // TR
                        paint.color = accentColor
                        paint.setShadowLayer(10f, 0f, 0f, accentColor)
                        canvas.nativeCanvas.drawLine(w - len, 0f, w, 0f, paint)
                        canvas.nativeCanvas.drawLine(w, 0f, w, len, paint)
                        // BL
                        paint.color = accentColor
                        canvas.nativeCanvas.drawLine(0f, h - len, 0f, h, paint)
                        canvas.nativeCanvas.drawLine(0f, h, len, h, paint)
                        // BR
                        paint.color = color
                        paint.setShadowLayer(10f, 0f, 0f, color)
                        canvas.nativeCanvas.drawLine(w - len, h, w, h, paint)
                        canvas.nativeCanvas.drawLine(w, h - len, w, h, paint)

                        // Animated scan line
                        val lineY = h * scanLine
                        val linePaint = android.graphics.Paint().apply {
                            shader = android.graphics.LinearGradient(
                                0f, lineY, w, lineY,
                                intArrayOf(0x0000F5FF.toInt(), 0xFF00F5FF.toInt(), 0x0000F5FF.toInt()),
                                null, android.graphics.Shader.TileMode.CLAMP
                            )
                            this.strokeWidth = 3f
                            this.style = android.graphics.Paint.Style.STROKE
                            setShadowLayer(12f, 0f, 0f, 0xFF00F5FF.toInt())
                        }
                        canvas.nativeCanvas.drawLine(0f, lineY, w, lineY, linePaint)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(scannerSize.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(GlassWhite, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassWhite2, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.QrCodeScanner, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Point camera at any WiFi QR code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScanTip(Icons.Rounded.PhoneAndroid, "Any Android")
                    ScanTip(Icons.Rounded.PhoneIphone, "Any iPhone")
                    ScanTip(Icons.Rounded.Apps, "Any App")
                }
            }
        }
    }
}

@Composable
private fun RowScope.ScanTip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(GlassWhite, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = NeonPurple, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun ScannedResultPanel(
    result: ScannedWifiResult,
    context: Context,
    viewModel: WifiViewModel,
    onScanAgain: () -> Unit,
    onNavigateGenerate: (WifiNetwork) -> Unit,
    onBack: () -> Unit
) {
    var saved by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    var showSaveOptions by remember { mutableStateOf(false) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val categories = listOf("Home", "Work", "Travel", "Public", "Guest", "General")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
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
            Text(
                "WiFi Detected!",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        // Success banner
        AnimatedVisibility(visible = true, enter = slideInVertically { -it } + fadeIn()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        Brush.linearGradient(listOf(NeonPurple.copy(0.25f), NeonCyan.copy(0.15f))),
                        RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, NeonPurple.copy(0.5f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "QR Code Decoded!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "All WiFi details extracted successfully",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Detail card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(CardSurface, RoundedCornerShape(24.dp))
                .border(1.dp, GlassWhite2, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "WiFi Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Security badge
                val (badgeColor, badgeText) = when (result.securityType) {
                    SecurityType.OPEN.name -> Pair(RedError, "OPEN")
                    SecurityType.WEP.name -> Pair(OrangeWarn, "WEP")
                    SecurityType.WPA3.name -> Pair(GreenSuccess, "WPA3")
                    else -> Pair(NeonCyan, "WPA2")
                }
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, badgeColor.copy(0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.Bold)
                }
            }

            DetailRow(
                label = "Network Name (SSID)",
                value = result.ssid,
                icon = Icons.Rounded.Wifi,
                iconColor = NeonPurple,
                onCopy = {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("SSID", result.ssid))
                }
            )

            DetailRow(
                label = "Password",
                value = if (showPassword) result.password.ifEmpty { "(No password / Open network)" }
                else if (result.password.isEmpty()) "(No password)" else "●".repeat(result.password.length.coerceAtMost(16)),
                icon = Icons.Rounded.Key,
                iconColor = NeonCyan,
                trailingIcon = if (result.password.isNotEmpty()) {
                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                } else null,
                onTrailingClick = { showPassword = !showPassword },
                onCopy = if (result.password.isNotEmpty()) {
                    {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("WiFi Password", result.password))
                    }
                } else null
            )

            DetailRow(
                label = "Security Protocol",
                value = try { SecurityType.valueOf(result.securityType).display } catch (e: Exception) { result.securityType },
                icon = Icons.Rounded.Shield,
                iconColor = OrangeWarn
            )

            DetailRow(
                label = "Hidden Network",
                value = if (result.isHidden) "Yes — SSID not broadcast" else "No — Network is visible",
                icon = if (result.isHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                iconColor = NeonPink
            )
        }

        Spacer(Modifier.height(20.dp))

        // Save options panel
        AnimatedVisibility(visible = showSaveOptions, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(CardSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, NeonPurple.copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Save Options", style = MaterialTheme.typography.titleSmall, color = NeonPurple, fontWeight = FontWeight.Bold)

                // Category picker
                Text("Category", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val catColor = when (cat) {
                            "Home" -> NeonPurple
                            "Work" -> NeonCyan
                            "Travel" -> OrangeWarn
                            "Public" -> NeonPink
                            "Guest" -> GreenSuccess
                            else -> TextSecondary
                        }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(0.25f),
                                selectedLabelColor = catColor,
                                containerColor = GlassWhite,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == cat,
                                selectedBorderColor = catColor,
                                borderColor = GlassWhite2
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. Coffee shop on Main St", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Notes, null, tint = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = airKeyTextFieldColors(),
                    maxLines = 2
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Toggle save options
            if (!saved) {
                TextButton(onClick = { showSaveOptions = !showSaveOptions }) {
                    Icon(
                        if (showSaveOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null, tint = TextSecondary, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (showSaveOptions) "Hide options" else "Add category & notes",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Save to vault
            Button(
                onClick = {
                    viewModel.saveNetwork(
                        WifiNetwork(
                            ssid = result.ssid,
                            password = result.password,
                            securityType = result.securityType,
                            isHidden = result.isHidden,
                            notes = notes,
                            category = selectedCategory
                        )
                    )
                    saved = true
                    showSaveOptions = false
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saved) GreenSuccess else NeonPurple
                ),
                enabled = !saved
            ) {
                Icon(if (saved) Icons.Rounded.CheckCircle else Icons.Rounded.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Saved to Vault!" else "Save to Vault", fontWeight = FontWeight.Bold)
            }

            // Generate custom QR
            OutlinedButton(
                onClick = {
                    onNavigateGenerate(
                        WifiNetwork(
                            ssid = result.ssid,
                            password = result.password,
                            securityType = result.securityType,
                            isHidden = result.isHidden,
                            notes = notes,
                            category = selectedCategory
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(NeonPurple, NeonCyan)))
            ) {
                Icon(Icons.Rounded.QrCode2, null, tint = NeonCyan)
                Spacer(Modifier.width(8.dp))
                Text("Generate Custom QR Code", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }

            // Open WiFi Settings (connect directly)
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GlassWhite2)
            ) {
                Icon(Icons.Rounded.NetworkWifi, null, tint = TextSecondary)
                Spacer(Modifier.width(8.dp))
                Text("Open WiFi Settings", color = TextSecondary)
            }

            // Scan again
            TextButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.QrCodeScanner, null, tint = TextMuted)
                Spacer(Modifier.width(8.dp))
                Text("Scan Another Code", color = TextMuted)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        if (trailingIcon != null && onTrailingClick != null) {
            IconButton(onClick = onTrailingClick, modifier = Modifier.size(36.dp)) {
                Icon(trailingIcon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ContentCopy, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "cam")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.9f, targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulse)
                .background(NeonPurple.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CameraAlt, null, tint = NeonPurple, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Camera Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "AirKey needs camera access to scan WiFi QR codes and extract network details instantly.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your camera is only used for QR scanning — nothing is recorded or transmitted.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
        ) {
            Icon(Icons.Rounded.CameraAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Go Back", color = TextSecondary)
        }
    }
}

private fun vibrate(context: Context) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 80, 40, 80), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            v.vibrate(100)
        }
    } catch (_: Exception) {}
}
