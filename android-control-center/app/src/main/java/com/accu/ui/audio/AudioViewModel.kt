package com.accu.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.data.db.dao.AudioPresetDao
import com.accu.data.db.entities.AudioPresetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioUiState(
    val dspEnabled: Boolean = false,
    val activePresetId: Long = -1,
    val activePresetName: String = "Default",
    val presets: List<PresetUiModel> = emptyList(),
    val eqBands: List<Float> = List(10) { 0f },
    val graphicEqEnabled: Boolean = false,
    val graphicEqBands: List<Float> = List(31) { 0f },
    val bassBoostStrength: Int = 0,
    val trebleBoostStrength: Int = 0,
    val stereoWideStrength: Float = 0f,
    val virtualizerStrength: Int = 0,
    val reverbPreset: Int = 0,
    val drcEnabled: Boolean = false,
    val drcGain: Float = 0f,
    val limiterEnabled: Boolean = false,
    val limiterGain: Float = 0f,
    val convolverEnabled: Boolean = false,
    val convolverIrPath: String = "",
    val liveprogEnabled: Boolean = false,
    val liveprogScript: String = "",
    val autoEqEnabled: Boolean = false,
    val autoEqProfileName: String = "",
    val targetPackages: List<String> = emptyList(),
    val snackbarMessage: String? = null,
)

data class PresetUiModel(val id: Long, val name: String, val isBuiltIn: Boolean = false)

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioPresetDao: AudioPresetDao,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioUiState())
    val state: StateFlow<AudioUiState> = _state.asStateFlow()

    init {
        loadPresets()
        insertBuiltInPresets()
    }

    private fun loadPresets() {
        viewModelScope.launch {
            audioPresetDao.observeAll().collect { list ->
                val models = list.map { PresetUiModel(it.id, it.name, it.isBuiltIn) }
                val active = list.firstOrNull { it.isEnabled }
                _state.update { s ->
                    var ns = s.copy(presets = models)
                    if (active != null) ns = ns.copy(activePresetId = active.id, activePresetName = active.name)
                    ns
                }
            }
        }
    }

    private fun insertBuiltInPresets() {
        viewModelScope.launch {
            val existing = audioPresetDao.observeAll().firstOrNull() ?: emptyList()
            if (existing.none { it.isBuiltIn }) {
                val builtIns = listOf(
                    AudioPresetEntity(name = "Flat", isBuiltIn = true),
                    AudioPresetEntity(name = "Bass Boost", isBuiltIn = true, bassBoostStrength = 600, eqBand0 = 4, eqBand1 = 3, eqBand2 = 2),
                    AudioPresetEntity(name = "Treble Boost", isBuiltIn = true, trebleBoostStrength = 500, eqBand8 = 3, eqBand9 = 4),
                    AudioPresetEntity(name = "V-Shape", isBuiltIn = true, bassBoostStrength = 400, trebleBoostStrength = 400, eqBand4 = -3, eqBand5 = -3),
                    AudioPresetEntity(name = "Vocal", isBuiltIn = true, eqBand3 = 2, eqBand4 = 3, eqBand5 = 3),
                    AudioPresetEntity(name = "Night Mode", isBuiltIn = true, drcEnabled = true, drcGain = -6f, limiterEnabled = true, limiterGain = -3f),
                    AudioPresetEntity(name = "Gaming", isBuiltIn = true, stereoWideStrength = 0.7f, virtualizerStrength = 600, bassBoostStrength = 300),
                )
                builtIns.forEach { audioPresetDao.insert(it) }
            }
        }
    }

    fun toggleDsp() { _state.update { it.copy(dspEnabled = !it.dspEnabled, snackbarMessage = if (!it.dspEnabled) "DSP enabled" else "DSP disabled") } }
    fun selectPreset(id: Long) {
        viewModelScope.launch {
            audioPresetDao.disableAll()
            audioPresetDao.enablePreset(id)
            val preset = audioPresetDao.observeAll().first().firstOrNull { it.id == id } ?: return@launch
            applyPresetToState(preset)
        }
    }

    private fun applyPresetToState(preset: AudioPresetEntity) {
        _state.update { s ->
            s.copy(
                activePresetId = preset.id,
                activePresetName = preset.name,
                eqBands = listOf(preset.eqBand0, preset.eqBand1, preset.eqBand2, preset.eqBand3, preset.eqBand4,
                    preset.eqBand5, preset.eqBand6, preset.eqBand7, preset.eqBand8, preset.eqBand9).map { it.toFloat() },
                bassBoostStrength = preset.bassBoostStrength,
                trebleBoostStrength = preset.trebleBoostStrength,
                stereoWideStrength = preset.stereoWideStrength,
                virtualizerStrength = preset.virtualizerStrength,
                reverbPreset = preset.reverbPreset,
                drcEnabled = preset.drcEnabled,
                drcGain = preset.drcGain,
                limiterEnabled = preset.limiterEnabled,
                limiterGain = preset.limiterGain,
                convolverEnabled = preset.convolverEnabled,
                convolverIrPath = preset.convolverImpulseResponse,
                liveprogEnabled = preset.liveprogEnabled,
                liveprogScript = preset.liveprogScript,
                autoEqEnabled = preset.autoEqEnabled,
                autoEqProfileName = preset.autoEqProfileName,
                targetPackages = preset.targetPackages,
            )
        }
    }

    fun saveCurrentPreset() {
        viewModelScope.launch {
            val s = _state.value
            val preset = AudioPresetEntity(
                name = "Custom ${System.currentTimeMillis()}",
                eqBand0 = s.eqBands.getOrElse(0) { 0f }.toInt(), eqBand1 = s.eqBands.getOrElse(1) { 0f }.toInt(),
                eqBand2 = s.eqBands.getOrElse(2) { 0f }.toInt(), eqBand3 = s.eqBands.getOrElse(3) { 0f }.toInt(),
                eqBand4 = s.eqBands.getOrElse(4) { 0f }.toInt(), eqBand5 = s.eqBands.getOrElse(5) { 0f }.toInt(),
                eqBand6 = s.eqBands.getOrElse(6) { 0f }.toInt(), eqBand7 = s.eqBands.getOrElse(7) { 0f }.toInt(),
                eqBand8 = s.eqBands.getOrElse(8) { 0f }.toInt(), eqBand9 = s.eqBands.getOrElse(9) { 0f }.toInt(),
                bassBoostStrength = s.bassBoostStrength, trebleBoostStrength = s.trebleBoostStrength,
                stereoWideStrength = s.stereoWideStrength, virtualizerStrength = s.virtualizerStrength,
                reverbPreset = s.reverbPreset, drcEnabled = s.drcEnabled, drcGain = s.drcGain,
                limiterEnabled = s.limiterEnabled, limiterGain = s.limiterGain,
                convolverEnabled = s.convolverEnabled, convolverImpulseResponse = s.convolverIrPath,
                liveprogEnabled = s.liveprogEnabled, liveprogScript = s.liveprogScript,
                autoEqEnabled = s.autoEqEnabled, autoEqProfileName = s.autoEqProfileName,
                targetPackages = s.targetPackages,
            )
            audioPresetDao.insert(preset)
            _state.update { it.copy(snackbarMessage = "Preset saved") }
        }
    }

    fun updateEqBand(index: Int, value: Float) { _state.update { s -> s.copy(eqBands = s.eqBands.toMutableList().also { it[index] = value }) } }
    fun toggleGraphicEq() { _state.update { it.copy(graphicEqEnabled = !it.graphicEqEnabled) } }
    fun updateGraphicEqBand(index: Int, value: Float) { _state.update { s -> s.copy(graphicEqBands = s.graphicEqBands.toMutableList().also { it[index] = value }) } }
    fun updateBassBoost(v: Int) { _state.update { it.copy(bassBoostStrength = v) } }
    fun updateTrebleBoost(v: Int) { _state.update { it.copy(trebleBoostStrength = v) } }
    fun updateStereoWidener(v: Float) { _state.update { it.copy(stereoWideStrength = v) } }
    fun updateVirtualizer(v: Int) { _state.update { it.copy(virtualizerStrength = v) } }
    fun updateReverbPreset(v: Int) { _state.update { it.copy(reverbPreset = v) } }
    fun toggleDrc() { _state.update { it.copy(drcEnabled = !it.drcEnabled) } }
    fun updateDrcGain(v: Float) { _state.update { it.copy(drcGain = v) } }
    fun toggleLimiter() { _state.update { it.copy(limiterEnabled = !it.limiterEnabled) } }
    fun updateLimiterGain(v: Float) { _state.update { it.copy(limiterGain = v) } }
    fun toggleConvolver() { _state.update { it.copy(convolverEnabled = !it.convolverEnabled) } }
    fun toggleLiveprog() { _state.update { it.copy(liveprogEnabled = !it.liveprogEnabled) } }
    fun updateLiveprogScript(s: String) { _state.update { it.copy(liveprogScript = s) } }
    fun toggleAutoEq() { _state.update { it.copy(autoEqEnabled = !it.autoEqEnabled) } }
    fun removeTargetPackage(pkg: String) { _state.update { it.copy(targetPackages = it.targetPackages - pkg) } }
    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }
}
