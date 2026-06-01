package com.accu.audio

import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log

class AccuAudioPlayer(
    streamType: Int,
    sampleRateInHz: Int,
    channelConfig: Int,
    audioFormat: Int,
    bufferSizeInBytes: Int,
    mode: Int,
) : AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode) {

    var volume: Float = 1f
    private var stereoGainL = 1f
    private var stereoGainR = 1f
    var savedBands = floatArrayOf(50f, 50f, 50f)

    private val equalizer: Equalizer by lazy { Equalizer(0, audioSessionId) }
    private val enhancer: LoudnessEnhancer by lazy { LoudnessEnhancer(audioSessionId) }
    private val suppress: NoiseSuppressor? by lazy {
        if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(audioSessionId) else null
    }
    private val echoCancel: AcousticEchoCanceler? by lazy {
        if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(audioSessionId) else null
    }

    fun setCurrentVolume(vol: Float) {
        volume = (vol / 100f).coerceIn(0f, 2f)
        val cappedVol = volume.coerceAtMost(1f)
        setStereoVolume(cappedVol * stereoGainL, cappedVol * stereoGainR)
        try {
            val boost = vol > 100
            enhancer.enabled = boost
            if (boost) enhancer.setTargetGain(((vol.toInt() - 100) * 150).coerceIn(0, 15000))
        } catch (_: Exception) { Log.i(TAG, "LoudnessEnhancer not supported") }
        try { suppress?.enabled = vol > 100 } catch (_: Exception) {}
        try { echoCancel?.enabled = vol > 100 } catch (_: Exception) {}
    }

    fun getBalance(): Float = (stereoGainL - stereoGainR) * -100f

    fun setBalance(value: Float) {
        stereoGainL = if (value <= 0) 1f else 1f - (value / 100f)
        stereoGainR = if (value >= 0) 1f else 1f + (value / 100f)
        setStereoVolume(volume.coerceAtMost(1f) * stereoGainL, volume.coerceAtMost(1f) * stereoGainR)
    }

    fun setBand(band: Int, value: Float) {
        if (band !in 0..2) return
        savedBands[band] = value
        try {
            equalizer.enabled = savedBands.any { it != 50f }
            val range = equalizer.bandLevelRange
            val span = range[1] - range[0]
            val freqRanges = arrayOf(0..250, 250..2000, 2000..20000)
            for (i in 0 until equalizer.numberOfBands) {
                val cf = equalizer.getCenterFreq(i.toShort()) / 1000
                if (cf in freqRanges[band]) {
                    val level = (range[0] + (span * value / 100f)).toInt()
                    equalizer.setBandLevel(i.toShort(), level.toShort().coerceIn(range[0], range[1]))
                }
            }
        } catch (_: Exception) { Log.i(TAG, "Equalizer not supported for band $band") }
    }

    fun releaseEffects() {
        try { equalizer.release() } catch (_: Exception) {}
        try { enhancer.release() } catch (_: Exception) {}
        try { suppress?.release() } catch (_: Exception) {}
        try { echoCancel?.release() } catch (_: Exception) {}
    }

    companion object {
        const val TAG = "AccuAudioPlayer"
    }
}
