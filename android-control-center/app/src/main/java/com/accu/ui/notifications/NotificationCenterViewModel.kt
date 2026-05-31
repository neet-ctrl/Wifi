package com.accu.ui.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.notifications.AccuNotificationHelper
import com.accu.notifications.ALL_NOTIFICATION_FEATURES
import com.accu.notifications.NotificationFeature
import com.accu.notifications.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ChannelUiState(
    val feature: NotificationFeature,
    val isEnabled: Boolean,
    val importanceOverride: Int?,
    val notifCount: Int,
    val lastFiredMs: Long,
    val isExpanded: Boolean = false,
    val testSent: Boolean = false,
)

data class NotificationCenterUiState(
    val masterEnabled: Boolean = true,
    val channels: List<ChannelUiState> = emptyList(),
    val snoozeUntilMs: Long = 0L,
    val hasPermission: Boolean = true,
    val searchQuery: String = "",
    val snackbarMessage: String? = null,
    val showSnoozeDialog: Boolean = false,
)

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: NotificationPreferences,
    private val helper: AccuNotificationHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationCenterUiState())
    val state: StateFlow<NotificationCenterUiState> = _state.asStateFlow()

    private val nm = context.getSystemService(NotificationManager::class.java)

    init { load() }

    fun load() {
        val hasPerm = if (android.os.Build.VERSION.SDK_INT >= 33)
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        else true

        val channels = ALL_NOTIFICATION_FEATURES.map { f ->
            ChannelUiState(
                feature = f,
                isEnabled = prefs.isChannelEnabled(f.channelId),
                importanceOverride = prefs.getImportanceOverride(f.channelId),
                notifCount = prefs.getCount(f.channelId),
                lastFiredMs = prefs.getLastMs(f.channelId),
            )
        }
        _state.update {
            it.copy(
                masterEnabled = prefs.masterEnabled,
                snoozeUntilMs = prefs.snoozeUntilMs,
                channels = channels,
                hasPermission = hasPerm,
            )
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.masterEnabled = enabled
        _state.update { it.copy(masterEnabled = enabled) }
        if (!enabled) _state.update { it.copy(snoozeUntilMs = 0L) }
    }

    fun setChannelEnabled(channelId: String, enabled: Boolean) {
        prefs.setChannelEnabled(channelId, enabled)
        _state.update { s ->
            s.copy(channels = s.channels.map { ch ->
                if (ch.feature.channelId == channelId) ch.copy(isEnabled = enabled) else ch
            })
        }
    }

    fun setImportance(channelId: String, importance: Int) {
        prefs.setImportanceOverride(channelId, importance)
        nm.getNotificationChannel(channelId)?.let { existing ->
            // re-create channel with new importance (Android allows only upward on re-create)
            val channel = android.app.NotificationChannel(
                channelId, existing.name, importance
            ).apply {
                description = existing.description
                lockscreenVisibility = existing.lockscreenVisibility
                setShowBadge(existing.canShowBadge())
            }
            nm.createNotificationChannel(channel)
        }
        _state.update { s ->
            s.copy(channels = s.channels.map { ch ->
                if (ch.feature.channelId == channelId) ch.copy(importanceOverride = importance) else ch
            })
        }
        _state.update { it.copy(snackbarMessage = "Importance updated. Some changes require restarting affected services.") }
    }

    fun toggleExpanded(channelId: String) {
        _state.update { s ->
            s.copy(channels = s.channels.map { ch ->
                if (ch.feature.channelId == channelId) ch.copy(isExpanded = !ch.isExpanded) else ch
            })
        }
    }

    fun sendTestNotification(channelId: String) {
        val feature = ALL_NOTIFICATION_FEATURES.find { it.channelId == channelId } ?: return
        viewModelScope.launch {
            // postTest bypasses the channel-disabled guard so the user can always verify delivery
            helper.postTest(channelId, System.currentTimeMillis().toInt() and 0x7FFF) {
                setContentTitle("Test: ${feature.featureName}")
                setContentText("This is a test notification from ACC Notification Center.")
                setAutoCancel(true)
                setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            }
            prefs.incrementCount(channelId)
            _state.update { s ->
                s.copy(
                    channels = s.channels.map { ch ->
                        if (ch.feature.channelId == channelId) ch.copy(testSent = true, notifCount = prefs.getCount(channelId)) else ch
                    },
                    snackbarMessage = "Test notification sent for ${feature.featureName}",
                )
            }
            delay(3000)
            _state.update { s ->
                s.copy(channels = s.channels.map { ch ->
                    if (ch.feature.channelId == channelId) ch.copy(testSent = false) else ch
                })
            }
        }
    }

    fun snoozeAll(hours: Int) {
        val until = System.currentTimeMillis() + hours * 3_600_000L
        prefs.snoozeUntilMs = until
        _state.update { it.copy(snoozeUntilMs = until, showSnoozeDialog = false, snackbarMessage = "Notifications snoozed for $hours hour${if (hours != 1) "s" else ""}") }
    }

    fun clearSnooze() {
        prefs.snoozeUntilMs = 0L
        _state.update { it.copy(snoozeUntilMs = 0L) }
    }

    fun resetToDefaults() {
        prefs.resetToDefaults()
        load()
        _state.update { it.copy(snackbarMessage = "All notification settings reset to defaults") }
    }

    fun enableAll() { prefs.enableAll(); load() }
    fun disableAll() { prefs.disableAll(); load() }

    fun onSearchChange(q: String) = _state.update { it.copy(searchQuery = q) }
    fun showSnoozeDialog() = _state.update { it.copy(showSnoozeDialog = true) }
    fun dismissSnoozeDialog() = _state.update { it.copy(showSnoozeDialog = false) }
    fun clearSnackbar() = _state.update { it.copy(snackbarMessage = null) }
}

fun formatRelativeTime(ms: Long): String {
    if (ms == 0L) return "Never"
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000L     -> "Just now"
        diff < 3_600_000L  -> "${diff / 60_000} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000} hr ago"
        else               -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ms))
    }
}

fun importanceLabel(imp: Int): String = when (imp) {
    NotificationManager.IMPORTANCE_MIN      -> "Silent"
    NotificationManager.IMPORTANCE_LOW      -> "Low"
    NotificationManager.IMPORTANCE_DEFAULT  -> "Normal"
    NotificationManager.IMPORTANCE_HIGH     -> "Urgent"
    NotificationManager.IMPORTANCE_MAX      -> "Critical"
    else                                    -> "Default"
}
