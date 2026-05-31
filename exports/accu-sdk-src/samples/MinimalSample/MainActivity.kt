package com.example.accu.minimal

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.accu.sdk.AccuClient
import com.accu.sdk.AccuConnectionState
import com.accu.sdk.AccuConstants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ACCU SDK — Minimal Sample
 *
 * The absolute minimum code to connect to ACCU and run a shell command.
 * No UI — output goes to Logcat (tag: ACCU_MINIMAL).
 *
 * Steps demonstrated:
 *  1. Create AccuClient
 *  2. Connect
 *  3. Wait for Connected state
 *  4. Request permission if needed
 *  5. Run "id" command
 *  6. Disconnect
 */
class MainActivity : ComponentActivity() {

    private val accu = AccuClient(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Step 1: Connect
            accu.connect()

            // Step 2: Wait until connected (or error)
            val connectedState = accu.state.first { it is AccuConnectionState.Connected || it is AccuConnectionState.Error }

            if (connectedState is AccuConnectionState.Error) {
                Log.e("ACCU_MINIMAL", "Failed to connect: ${connectedState.reason}")
                return@launch
            }

            Log.d("ACCU_MINIMAL", "Connected to ACCU!")

            // Step 3: Check permission
            var permCode = accu.checkPermission()
            Log.d("ACCU_MINIMAL", "Permission code: $permCode")

            // Step 4: Request permission if not yet granted
            if (permCode != AccuConstants.PERMISSION_GRANTED) {
                Log.d("ACCU_MINIMAL", "Requesting permission — ACCU dialog will appear...")
                permCode = accu.requestPermission()
                Log.d("ACCU_MINIMAL", "Permission result: $permCode")
            }

            if (permCode != AccuConstants.PERMISSION_GRANTED) {
                Log.e("ACCU_MINIMAL", "Permission not granted. Cannot continue.")
                return@launch
            }

            // Step 5: Run a shell command
            val result = accu.exec("id && uname -a")
            Log.d("ACCU_MINIMAL", "stdout: ${result.stdout}")
            Log.d("ACCU_MINIMAL", "stderr: ${result.stderr}")
            Log.d("ACCU_MINIMAL", "exitCode: ${result.exitCode}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        accu.disconnect()  // Step 6: Always disconnect
    }
}
