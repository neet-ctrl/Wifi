package com.accu.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.accu.services.CallRecordingService
import timber.log.Timber

/**
 * Monitors phone call state and triggers call recording service.
 * Based on ShizuCallRecorder's PhoneStateReceiver approach.
 */
class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var savedNumber = ""
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

        Timber.d("Call state: $state, number: $number")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                isIncoming = true
                savedNumber = number
                lastState = TelephonyManager.CALL_STATE_RINGING
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false
                }
                lastState = TelephonyManager.CALL_STATE_OFFHOOK
                // Call started — begin recording
                startRecordingIfEnabled(context, savedNumber, isIncoming)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING ||
                    lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Call ended — stop recording
                    if (CallRecordingService.isRecording) {
                        CallRecordingService.stop(context)
                    }
                }
                lastState = TelephonyManager.CALL_STATE_IDLE
                savedNumber = ""
            }
        }
    }

    private fun startRecordingIfEnabled(context: Context, number: String, isIncoming: Boolean) {
        // Check if recording is enabled in preferences
        val prefs = context.getSharedPreferences("acc_prefs", Context.MODE_PRIVATE)
        val recordingEnabled = prefs.getBoolean("call_recording_enabled", false)
        if (recordingEnabled) {
            Timber.i("Starting call recording for ${if (isIncoming) "incoming" else "outgoing"} call")
            CallRecordingService.start(context)
        }
    }
}
