package com.yourcompany.yourapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.accu.sdk.AccuConnectionState
import com.accu.sdk.AccuConstants

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              ACCU SDK — MainActivity Template                            ║
 * ║                                                                          ║
 * ║  Demonstrates connecting to ACCU and requesting permission               ║
 * ║  using Jetpack Compose + the AccuViewModel template.                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AccuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AccuDemoScreen(viewModel)
            }
        }
    }
}

@Composable
fun AccuDemoScreen(viewModel: AccuViewModel) {
    val state by viewModel.accuState.collectAsState()
    var shellResult by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ACCU SDK Demo", style = MaterialTheme.typography.headlineMedium)

        // ── Connection State Card ─────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Connection State", style = MaterialTheme.typography.titleSmall)
                when (val s = state) {
                    is AccuConnectionState.Idle        -> Text("⚪ Idle — not connected yet")
                    is AccuConnectionState.Connecting  -> { Text("🟡 Connecting…"); LinearProgressIndicator() }
                    is AccuConnectionState.Connected   -> {
                        Text("🟢 Connected — ACCU ${s.accuVersion} (API v${s.serviceVersion})")
                        val permLabel = when (s.permissionCode) {
                            AccuConstants.PERMISSION_GRANTED           -> "✅ Permission: GRANTED"
                            AccuConstants.PERMISSION_DENIED            -> "❌ Permission: DENIED"
                            AccuConstants.PERMISSION_NOT_YET_REQUESTED -> "⚠ Permission: Not yet requested"
                            else                                       -> "? Permission: ${s.permissionCode}"
                        }
                        Text(permLabel)
                    }
                    is AccuConnectionState.Disconnected -> Text("🔴 Disconnected — reconnecting…")
                    is AccuConnectionState.Error        -> Text("💔 Error: ${s.reason}")
                }
            }
        }

        // ── Request Permission ────────────────────────────────────────────
        if (state is AccuConnectionState.Connected &&
            (state as AccuConnectionState.Connected).permissionCode != AccuConstants.PERMISSION_GRANTED) {
            Button(onClick = {
                viewModel.requestAccuPermission { code ->
                    statusMessage = when (code) {
                        AccuConstants.PERMISSION_GRANTED -> "Permission granted!"
                        AccuConstants.PERMISSION_DENIED  -> "Permission denied by user."
                        else                              -> "Permission result: $code"
                    }
                }
            }) {
                Text("Request ACCU Permission")
            }
        }

        // ── Run Shell Command (example) ───────────────────────────────────
        if (viewModel.isReady) {
            Button(onClick = {
                viewModel.runShellCommand("id && uname -a") { result ->
                    shellResult = if (result.isSuccess) result.stdout
                    else "Error (exit ${result.exitCode}): ${result.stderr}"
                }
            }) {
                Text("Run: id && uname -a")
            }

            if (shellResult.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = shellResult,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
