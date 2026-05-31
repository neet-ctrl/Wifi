package com.accu.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "accu_notification_prefs"
private const val KEY_MASTER  = "master_notifications_enabled"
private const val KEY_SNOOZE_UNTIL = "snooze_until_ms"

/**
 * SharedPreferences-backed store for per-channel and master notification preferences.
 * Kept lightweight — no DataStore dependency, no coroutines required at read time.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Master toggle ─────────────────────────────────────────────
    var masterEnabled: Boolean
        get() = sp.getBoolean(KEY_MASTER, true)
        set(value) { sp.edit().putBoolean(KEY_MASTER, value).apply() }

    // ── Snooze ────────────────────────────────────────────────────
    var snoozeUntilMs: Long
        get() = sp.getLong(KEY_SNOOZE_UNTIL, 0L)
        set(value) { sp.edit().putLong(KEY_SNOOZE_UNTIL, value).apply() }

    val isSnoozed: Boolean get() = System.currentTimeMillis() < snoozeUntilMs

    // ── Per-channel enable ────────────────────────────────────────
    fun isChannelEnabled(channelId: String): Boolean {
        if (!masterEnabled || isSnoozed) return false
        val default = ALL_NOTIFICATION_FEATURES.find { it.channelId == channelId }?.defaultEnabled ?: true
        return sp.getBoolean("ch_enabled_$channelId", default)
    }

    fun setChannelEnabled(channelId: String, enabled: Boolean) {
        sp.edit().putBoolean("ch_enabled_$channelId", enabled).apply()
    }

    // ── Per-channel importance override ──────────────────────────
    fun getImportanceOverride(channelId: String): Int? {
        val v = sp.getInt("ch_importance_$channelId", -1)
        return if (v == -1) null else v
    }

    fun setImportanceOverride(channelId: String, importance: Int) {
        sp.edit().putInt("ch_importance_$channelId", importance).apply()
    }

    // ── Per-channel stats ─────────────────────────────────────────
    fun incrementCount(channelId: String) {
        val key = "ch_count_$channelId"
        sp.edit().putInt(key, sp.getInt(key, 0) + 1).apply()
        sp.edit().putLong("ch_last_ms_$channelId", System.currentTimeMillis()).apply()
    }

    fun getCount(channelId: String): Int = sp.getInt("ch_count_$channelId", 0)
    fun getLastMs(channelId: String): Long = sp.getLong("ch_last_ms_$channelId", 0L)

    // ── Bulk operations ───────────────────────────────────────────
    fun enableAll() {
        val editor = sp.edit()
        ALL_NOTIFICATION_FEATURES.forEach { editor.putBoolean("ch_enabled_${it.channelId}", true) }
        editor.putBoolean(KEY_MASTER, true)
        editor.putLong(KEY_SNOOZE_UNTIL, 0L)
        editor.apply()
    }

    fun disableAll() {
        val editor = sp.edit()
        ALL_NOTIFICATION_FEATURES.forEach { editor.putBoolean("ch_enabled_${it.channelId}", false) }
        editor.apply()
    }

    fun resetToDefaults() {
        val editor = sp.edit()
        ALL_NOTIFICATION_FEATURES.forEach { f ->
            editor.putBoolean("ch_enabled_${f.channelId}", f.defaultEnabled)
            editor.remove("ch_importance_${f.channelId}")
        }
        editor.putBoolean(KEY_MASTER, true)
        editor.putLong(KEY_SNOOZE_UNTIL, 0L)
        editor.apply()
    }
}
