package com.airkey.wifiqr.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val storageGranted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else true
    }
    var storageGrantedState by remember { mutableStateOf(storageGranted) }

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
                Text("Permissions & App Info", style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
