package com.airkey.wifiqr.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.ui.theme.*
import com.airkey.wifiqr.viewmodel.WifiViewModel
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(viewModel: WifiViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val storageGranted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else true
    }
    var storageGrantedState by remember { mutableStateOf(storageGranted) }

    val backupMessage by viewModel.backupMessage.collectAsState()
    val pdfExportMessage by viewModel.pdfExportMessage.collectAsState()
    var autoBackupDays by remember { mutableIntStateOf(1) }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.backupNetworks(context, uri)
        }
    }

    val pdfFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.exportPdfBooklet(context, uri)
        }
    }

    val autoBackupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.scheduleAutoBackup(context, uri, autoBackupDays)
        }
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreNetworks(context, uri)
        }
    }

    LaunchedEffect(backupMessage) {
        if (backupMessage != null) {
            delay(5000)
            viewModel.clearBackupMessage()
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Permissions, Backup & App Info", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SettingsSectionLabel("App Permissions", Icons.Rounded.Security, NeonCyan)

            PermissionCard(
                icon = Icons.Rounded.CameraAlt,
                title = "Camera",
                description = "Required to scan WiFi QR codes using your camera",
                isGranted = cameraPermission.status.isGranted,
                accentColor = NeonPurple,
                onGrant = { cameraPermission.launchPermissionRequest() },
                onOpenSettings = { openAppSettings(context) }
            )

            PermissionCard(
                icon = Icons.Rounded.LocationOn,
                title = "Location (Fine)",
                description = "Required on Android 8+ to scan for nearby WiFi networks",
                isGranted = locationPermission.status.isGranted,
                accentColor = NeonCyan,
                onGrant = { locationPermission.launchPermissionRequest() },
                onOpenSettings = { openAppSettings(context) }
            )

            PermissionCard(
                icon = Icons.Rounded.FolderOpen,
                title = "Manage All Files",
                description = "Lets you pick any folder to save QR codes into",
                isGranted = storageGrantedState,
                accentColor = NeonPink,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                        storageGrantedState = Environment.isExternalStorageManager()
                    }
                },
                onOpenSettings = { openAppSettings(context) }
            )

            PermissionCard(
                icon = Icons.Rounded.Wifi,
                title = "WiFi State",
                description = "Used to suggest connections from your saved network vault",
                isGranted = true,
                accentColor = GreenSuccess,
                onGrant = {},
                onOpenSettings = {},
                autoGranted = true
            )

            PermissionCard(
                icon = Icons.Rounded.Vibration,
                title = "Vibration",
                description = "Haptic feedback when a QR code is detected",
                isGranted = true,
                accentColor = OrangeWarn,
                onGrant = {},
                onOpenSettings = {},
                autoGranted = true
            )

            Spacer(Modifier.height(8.dp))
            SettingsSectionLabel("PDF Export", Icons.Rounded.PictureAsPdf, NeonPink)

            AnimatedVisibility(
                visible = pdfExportMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val isOk = pdfExportMessage?.startsWith("✓") == true
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (isOk) GreenSuccess.copy(0.12f) else Color(0xFFFF4444).copy(0.12f))
                        .border(1.dp, if (isOk) GreenSuccess.copy(0.4f) else Color(0xFFFF4444).copy(0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isOk) Icons.Rounded.CheckCircle else Icons.Rounded.Error, null,
                        tint = if (isOk) GreenSuccess else Color(0xFFFF6666), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(pdfExportMessage ?: "", color = if (isOk) GreenSuccess else Color(0xFFFF6666),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }

            BackupActionCard(
                icon = Icons.Rounded.PictureAsPdf,
                title = "Export QR Booklet as PDF",
                description = "Creates a printable PDF with one QR code per page for all your saved networks — perfect for offices, hotels, or home labels",
                accentColor = NeonPink,
                buttonLabel = "Choose Folder & Export PDF",
                buttonIcon = Icons.Rounded.FolderOpen,
                onClick = { pdfFolderLauncher.launch(null) }
            )

            Spacer(Modifier.height(8.dp))
            SettingsSectionLabel("Data Backup", Icons.Rounded.Backup, GreenSuccess)

            AnimatedVisibility(
                visible = backupMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val isSuccess = backupMessage?.startsWith("✓") == true || backupMessage?.startsWith("Restored") == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSuccess) GreenSuccess.copy(alpha = 0.12f)
                            else Color(0xFFFF4444).copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isSuccess) GreenSuccess.copy(0.4f) else Color(0xFFFF4444).copy(0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                        null,
                        tint = if (isSuccess) GreenSuccess else Color(0xFFFF6666),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        backupMessage ?: "",
                        color = if (isSuccess) GreenSuccess else Color(0xFFFF6666),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            BackupActionCard(
                icon = Icons.Rounded.CloudUpload,
                title = "Create Backup",
                description = "Saves all your WiFi networks & QR codes into a single .wifi file in any folder you choose",
                accentColor = NeonPurple,
                buttonLabel = "Choose Folder & Backup",
                buttonIcon = Icons.Rounded.FolderOpen,
                onClick = { backupFolderLauncher.launch(null) }
            )

            BackupActionCard(
                icon = Icons.Rounded.CloudDownload,
                title = "Restore Backup",
                description = "Pick a .wifi backup file — all networks are merged with your existing data, nothing is overwritten",
                accentColor = NeonCyan,
                buttonLabel = "Select .wifi File",
                buttonIcon = Icons.Rounded.FileOpen,
                onClick = { restoreFileLauncher.launch(arrayOf("*/*")) }
            )

            Spacer(Modifier.height(8.dp))
            SettingsSectionLabel("Auto-Backup Schedule", Icons.Rounded.Schedule, OrangeWarn)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(CardSurface, DarkSurface.copy(0.97f))))
                    .border(1.dp, OrangeWarn.copy(0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, null, tint = OrangeWarn, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Backup Interval", style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Choose how often AirKey automatically saves a backup to your chosen folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    val intervalOptions = listOf(1 to "Every day", 3 to "Every 3 days", 7 to "Every week", 14 to "Every 2 weeks", 30 to "Every month")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervalOptions.forEach { (days, label) ->
                            val selected = autoBackupDays == days
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) OrangeWarn.copy(0.25f) else GlassWhite)
                                    .border(1.dp, if (selected) OrangeWarn.copy(0.8f) else GlassWhite2, RoundedCornerShape(10.dp))
                                    .clickable { autoBackupDays = days }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) OrangeWarn else TextSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { autoBackupFolderLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeWarn.copy(0.18f), contentColor = OrangeWarn),
                            border = BorderStroke(1.dp, OrangeWarn.copy(0.55f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Set Folder & Schedule", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { viewModel.cancelAutoBackup(context) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedError.copy(0.12f), contentColor = RedError),
                            border = BorderStroke(1.dp, RedError.copy(0.4f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SettingsSectionLabel("About AirKey", Icons.Rounded.Info, NeonPurple)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .coloredShadow(NeonPurple, 20.dp, 16.dp, alpha = 0.25f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(CardSurface, DarkSurface.copy(alpha = 0.95f))))
                    .border(1.dp, Brush.linearGradient(listOf(NeonPurple.copy(0.4f), NeonCyan.copy(0.3f))), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppInfoRow(Icons.Rounded.Apps, "App Name", "AirKey — WiFi QR Manager", NeonPurple)
                    AppInfoRow(Icons.Rounded.Person, "Developer", "Shakti Kumar", NeonCyan)
                    AppInfoRow(Icons.Rounded.Code, "Version", "1.0.0", GreenSuccess)
                    AppInfoRow(Icons.Rounded.Shield, "Privacy", "All data stored privately on-device only", OrangeWarn)

                    HorizontalDivider(color = GlassWhite2, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonPurple.copy(0.1f))
                            .border(1.dp, NeonPurple.copy(0.3f), RoundedCornerShape(12.dp))
                            .clickable { uriHandler.openUri("https://github.com/neet-ctrl/Wifi") }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Source Code", style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("github.com/neet-ctrl/Wifi", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun BackupActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    buttonLabel: String,
    buttonIcon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .coloredShadow(accentColor, 16.dp, 12.dp, alpha = 0.18f)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(CardSurface, DarkSurface.copy(alpha = 0.95f))))
            .border(1.dp, accentColor.copy(0.35f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accentColor.copy(0.15f), CircleShape)
                    .border(1.dp, accentColor.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.85f))
        ) {
            Icon(buttonIcon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(buttonLabel, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String, icon: ImageVector, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(GlassWhite2, RoundedCornerShape(1.dp)))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    accentColor: Color,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    autoGranted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .coloredShadow(accentColor, 14.dp, 10.dp, alpha = 0.15f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(CardSurface, DarkSurface.copy(alpha = 0.95f))))
            .border(
                1.dp,
                if (isGranted) accentColor.copy(0.35f) else Color(0xFFFF4444).copy(0.4f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accentColor.copy(0.15f), CircleShape)
                .border(1.dp, accentColor.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        when {
            isGranted -> Icon(Icons.Rounded.CheckCircle, null, tint = if (autoGranted) accentColor else GreenSuccess, modifier = Modifier.size(24.dp))
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(
                    onClick = onGrant,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) { Text("Grant", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun AppInfoRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )
}
