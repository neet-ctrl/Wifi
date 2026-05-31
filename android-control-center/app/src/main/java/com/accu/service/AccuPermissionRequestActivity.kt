package com.accu.service

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accu.ui.theme.ACCTheme
import com.accu.ui.theme.AccentGreen
import com.accu.ui.theme.AccentOrange
import com.accu.ui.theme.AccentRed

/**
 * Full-screen permission grant dialog shown whenever an app calls
 * IAccuService.requestPermission() for the first time.
 *
 * Shows:
 *  • App name and package
 *  • List of what access ACCU will grant
 *  • Scope selector (allow user to narrow permissions)
 *  • Grant / Deny buttons
 */
class AccuPermissionRequestActivity : ComponentActivity() {

    companion object {
        private var serviceInstance: AccuSystemService? = null
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            // AccuSystemService is not a remote service — same process direct cast
            try {
                val field = binder.javaClass.getDeclaredField("this$0")
                field.isAccessible = true
                serviceInstance = field.get(binder) as? AccuSystemService
            } catch (_: Exception) { }
        }
        override fun onServiceDisconnected(name: ComponentName) { serviceInstance = null }
    }

    private lateinit var requestedPackage: String
    private lateinit var requestedLabel: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestedPackage = intent.getStringExtra(AccuSystemService.EXTRA_GRANT_PKG) ?: run { finish(); return }
        requestedLabel   = intent.getStringExtra(AccuSystemService.EXTRA_GRANT_LABEL) ?: requestedPackage

        // Bind to the running service to call grant/deny on it directly
        bindService(
            Intent(this, AccuSystemService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )

        setContent {
            ACCTheme {
                PermissionRequestScreen(
                    packageName  = requestedPackage,
                    appLabel     = requestedLabel,
                    appIcon      = getAppIcon(requestedPackage),
                    onGrant = { scopes ->
                        serviceInstance?.grantFromActivity(requestedPackage, scopes)
                            ?: run {
                                // Fallback: send intent to service
                                startService(Intent(this, AccuSystemService::class.java).apply {
                                    action = AccuSystemService.ACTION_GRANT
                                    putExtra(AccuSystemService.EXTRA_GRANT_PKG, requestedPackage)
                                    putExtra(AccuSystemService.EXTRA_GRANT_LABEL, requestedLabel)
                                })
                            }
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onDeny = {
                        serviceInstance?.denyFromActivity(requestedPackage)
                            ?: run {
                                startService(Intent(this, AccuSystemService::class.java).apply {
                                    action = AccuSystemService.ACTION_DENY
                                    putExtra(AccuSystemService.EXTRA_GRANT_PKG, requestedPackage)
                                })
                            }
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unbindService(connection) } catch (_: Exception) {}
    }

    private fun getAppIcon(packageName: String): androidx.compose.ui.graphics.ImageBitmap? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionRequestScreen(
    packageName: String,
    appLabel: String,
    appIcon: androidx.compose.ui.graphics.ImageBitmap?,
    onGrant: (Set<String>) -> Unit,
    onDeny: () -> Unit,
) {
    var grantShell         by remember { mutableStateOf(true) }
    var grantPackageMgmt   by remember { mutableStateOf(true) }
    var grantPermissions   by remember { mutableStateOf(true) }
    var grantSettings      by remember { mutableStateOf(true) }
    var grantLocale        by remember { mutableStateOf(true) }

    val selectedScopes = buildSet {
        if (grantShell)        add(SCOPE_SHELL)
        if (grantPackageMgmt)  add(SCOPE_PACKAGE_MANAGE)
        if (grantPermissions)  add(SCOPE_PERMISSIONS)
        if (grantSettings)     add(SCOPE_SETTINGS)
        if (grantLocale)       add(SCOPE_LOCALE)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {

                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // App icon placeholder
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Android, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(appLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Warning banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentOrange.copy(0.12f),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = AccentOrange)
                        Column {
                            Text("ACCU Privilege Request", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentOrange)
                            Text("This app is requesting elevated system access through ACCU. Only grant if you trust this app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Requested access:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                // Scope toggles
                ScopeRow(Icons.Default.Terminal,      "Shell Commands",      "Run any sh -c command with elevated privileges",                      grantShell)        { grantShell = it }
                ScopeRow(Icons.Default.Apps,          "Package Management",  "Install, uninstall, enable, disable, hide apps",                      grantPackageMgmt)  { grantPackageMgmt = it }
                ScopeRow(Icons.Default.Security,      "Permissions",         "Grant/revoke runtime permissions, control App Ops",                   grantPermissions)  { grantPermissions = it }
                ScopeRow(Icons.Default.Settings,      "System Settings",     "Write Settings.Secure / Settings.Global / Settings.System",           grantSettings)     { grantSettings = it }
                ScopeRow(Icons.Default.Translate,     "Locale Control",      "Set per-app language overrides",                                      grantLocale)       { grantLocale = it }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDeny,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    ) {
                        Icon(Icons.Default.Block, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Deny")
                    }
                    Button(
                        onClick = { onGrant(selectedScopes) },
                        modifier = Modifier.weight(2f),
                        enabled = selectedScopes.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Grant ${if (selectedScopes.size == ALL_SCOPES.size) "Full Access" else "${selectedScopes.size} Scopes"}", fontWeight = FontWeight.Bold)
                    }
                }

                // System insets bottom padding
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun ScopeRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
