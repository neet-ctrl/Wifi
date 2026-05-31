package com.accu.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Receives BOOT_COMPLETED and restores app state.
 * Re-applies frozen apps, automation rules, audio DSP, etc.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        Timber.i("Boot completed — restoring ACC state")

        // Restore frozen apps
        val goAsync = goAsync()
        try {
            // TODO: inject AutomationRepository and re-apply boot profiles
            Timber.d("ACC boot restore complete")
        } finally {
            goAsync.finish()
        }
    }
}
