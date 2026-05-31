package com.accu.ui.customization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.data.db.dao.CustomThemeDao
import com.accu.data.db.entities.CustomThemeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomizationUiState(
    val monetStyle: String = "TONAL_SPOT",
    val seedColor: Int = 0xFF4A56E2.toInt(),
    val pitchBlack: Boolean = false,
    val accurateShades: Boolean = true,
    val iconShape: String = "Squircle",
    val transparentStatusBar: Boolean = false,
    val transparentNavBar: Boolean = false,
    val hideNotch: Boolean = false,
    val fontScale: Float = 1.0f,
    val savedThemes: List<CustomThemeEntity> = emptyList(),
    val snackbarMessage: String? = null,
)

@HiltViewModel
class CustomizationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customThemeDao: CustomThemeDao,
) : ViewModel() {
    private val _state = MutableStateFlow(CustomizationUiState())
    val state: StateFlow<CustomizationUiState> = _state.asStateFlow()

    init { viewModelScope.launch { customThemeDao.observeAll().collect { themes -> _state.update { it.copy(savedThemes = themes) } } } }

    fun setMonetStyle(style: String) { _state.update { it.copy(monetStyle = style) } }
    fun setSeedColor(color: Int) { _state.update { it.copy(seedColor = color) } }
    fun togglePitchBlack(v: Boolean) { _state.update { it.copy(pitchBlack = v) } }
    fun toggleAccurateShades(v: Boolean) { _state.update { it.copy(accurateShades = v) } }
    fun setIconShape(shape: String) { _state.update { it.copy(iconShape = shape) } }
    fun toggleTransparentStatusBar(v: Boolean) { _state.update { it.copy(transparentStatusBar = v) } }
    fun toggleTransparentNavBar(v: Boolean) { _state.update { it.copy(transparentNavBar = v) } }
    fun toggleHideNotch(v: Boolean) { _state.update { it.copy(hideNotch = v) } }
    fun setFontScale(v: Float) { _state.update { it.copy(fontScale = v) } }

    fun applyTheme(id: Long = -1) {
        viewModelScope.launch {
            if (id > 0) {
                customThemeDao.clearApplied()
                customThemeDao.update(customThemeDao.observeAll().first().first { it.id == id }.copy(isApplied = true))
            } else {
                val s = _state.value
                customThemeDao.clearApplied()
                customThemeDao.insert(CustomThemeEntity(name = "Custom ${System.currentTimeMillis()}", monetStyle = s.monetStyle, seedColor = s.seedColor, accurateShades = s.accurateShades, pitchBlackTheme = s.pitchBlack, isApplied = true))
            }
            _state.update { it.copy(snackbarMessage = "Theme applied — restart may be needed") }
        }
    }

    fun deleteTheme(id: Long) {
        viewModelScope.launch {
            val theme = customThemeDao.observeAll().first().firstOrNull { it.id == id } ?: return@launch
            customThemeDao.delete(theme)
        }
    }
}
