package com.accu.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.accu.service.AccuSystemService
import timber.log.Timber

/**
 * Receives BOOT_COMPLETED and restores app state.
 *
 * Responsibilities:
 *  1. Auto-start AccuSystemService if the user has enabled "Boot autostart".
 *  2. Re-applies frozen apps, automation rules, audio DSP, etc. (future TODO).
 *
 * Boot autostart preference: SharedPreferences "accu_service_prefs", key "accu_service_autostart" (Boolean).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Timber.i("BootReceiver: ${intent.action}")

        maybeAutoStartService(context)
    }

    private fun maybeAutoStartService(context: Context) {
        val prefs = context.getSharedPreferences(
            AccuSystemService.PREFS_SERVICE, Context.MODE_PRIVATE
        )
        val autostart = prefs.getBoolean(AccuSystemService.PREF_AUTOSTART, false)
        if (autostart) {
            Timber.i("Boot autostart enabled — starting AccuSystemService")
            try {
                val serviceIntent = Intent(context, AccuSystemService::class.java)
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Timber.e(e, "BootReceiver: failed to start AccuSystemService")
            }
        } else {
            Timber.d("Boot autostart disabled — skipping AccuSystemService start")
        }
    }
}
