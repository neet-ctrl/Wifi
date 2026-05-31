package com.accu.ui.callrecorder

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.data.db.dao.CallRecordingDao
import com.accu.data.db.entities.CallRecordingEntity
import com.accu.services.CallRecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class CallRecorderUiState(
    val recordings: List<CallRecordingEntity> = emptyList(),
    val isRecordingEnabled: Boolean = false,
    val isCurrentlyRecording: Boolean = false,
    val recordingFormat: String = "AAC",
    val audioSource: String = "VOICE_CALL",
    val outputDirectory: String = "/sdcard/CallRecordings",
    val totalSizeBytes: Long = 0L,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val snackbarMessage: String? = null,
    val showSearch: Boolean = false,
    val showSettingsPanel: Boolean = false,
)

@HiltViewModel
class CallRecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRecordingDao: CallRecordingDao,
) : ViewModel() {

    private val _state = MutableStateFlow(CallRecorderUiState())
    val state: StateFlow<CallRecorderUiState> = _state.asStateFlow()

    init {
        observeRecordings()
        loadStats()
    }

    private fun observeRecordings() {
        viewModelScope.launch {
            callRecordingDao.observeAll().collect { recs ->
                _state.update { it.copy(recordings = recs, isLoading = false) }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val total = callRecordingDao.totalSizeBytes() ?: 0L
            _state.update { it.copy(totalSizeBytes = total) }
        }
    }

    fun toggleRecording() {
        val enabled = !_state.value.isRecordingEnabled
        _state.update { it.copy(isRecordingEnabled = enabled) }
        try {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = if (enabled) CallRecordingService.ACTION_START else CallRecordingService.ACTION_STOP
                putExtra(CallRecordingService.EXTRA_FORMAT, _state.value.recordingFormat)
                putExtra(CallRecordingService.EXTRA_SOURCE, _state.value.audioSource)
                putExtra(CallRecordingService.EXTRA_OUTPUT_DIR, _state.value.outputDirectory)
            }
            if (enabled) context.startForegroundService(intent) else context.stopService(intent)
            _state.update { it.copy(snackbarMessage = if (enabled) "Call recording enabled" else "Call recording disabled") }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle CallRecordingService")
            _state.update { it.copy(isRecordingEnabled = !enabled, snackbarMessage = "Service error: ${e.message}") }
        }
    }

    fun setFormat(format: String) { _state.update { it.copy(recordingFormat = format) } }
    fun setAudioSource(source: String) { _state.update { it.copy(audioSource = source) } }

    fun playRecording(recording: CallRecordingEntity) {
        try {
            val file = File(recording.filePath)
            if (!file.exists()) { _state.update { it.copy(snackbarMessage = "File not found: ${recording.filePath}") }; return }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e)
            _state.update { it.copy(snackbarMessage = "Cannot play: ${e.message}") }
        }
    }

    fun toggleStar(recording: CallRecordingEntity) {
        viewModelScope.launch {
            callRecordingDao.update(recording.copy(isStarred = !recording.isStarred))
        }
    }

    fun deleteRecording(recording: CallRecordingEntity) {
        viewModelScope.launch {
            try {
                File(recording.filePath).delete()
            } catch (_: Exception) {}
            callRecordingDao.delete(recording)
            loadStats()
            _state.update { it.copy(snackbarMessage = "Recording deleted") }
        }
    }

    fun shareRecording(recording: CallRecordingEntity) {
        try {
            val file = File(recording.filePath)
            if (!file.exists()) { _state.update { it.copy(snackbarMessage = "File not found") }; return }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Recording").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }
    fun toggleSearch() { _state.update { it.copy(showSearch = !it.showSearch) } }
    fun toggleSettingsPanel() { _state.update { it.copy(showSettingsPanel = !it.showSettingsPanel) } }
}
