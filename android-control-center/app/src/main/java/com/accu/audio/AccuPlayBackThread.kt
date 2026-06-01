package com.accu.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AccuPlayBackThread(
    val context: Context,
    val pkg: String,
    private val mediaProjection: MediaProjection,
    private val onAudioDenied: suspend (String) -> Unit = {},
    private val onAudioAllow: suspend (String) -> Unit = {},
) : Thread("AccuSoundMaster:$pkg") {

    @Volatile var playback = true
    var loadedCycles = 0

    private val sampleRate = 48000
    private val channel = AudioFormat.CHANNEL_IN_STEREO or AudioFormat.CHANNEL_OUT_STEREO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private val bufSize = AudioRecord.getMinBufferSize(sampleRate,
        AudioFormat.CHANNEL_IN_STEREO, encoding).coerceAtLeast(4096)
    private val dataBuffer = ByteArray(bufSize)

    private lateinit var mCapture: AudioRecord
    val mPlayers = hashMapOf<Int, AccuAudioPlayer>()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun run() {
        if (ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO not granted for $pkg")
            interrupt()
            return
        }
        try {
            val allUsages = listOf(
                AudioAttributes.USAGE_MEDIA,
                AudioAttributes.USAGE_GAME,
                AudioAttributes.USAGE_ALARM,
                AudioAttributes.USAGE_NOTIFICATION,
                AudioAttributes.USAGE_ASSISTANT,
                AudioAttributes.USAGE_UNKNOWN,
                AudioAttributes.USAGE_VOICE_COMMUNICATION,
            )
            val configBuilder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            allUsages.forEach { configBuilder.addMatchingUsage(it) }
            val uid = getAppUid(context, pkg)
            if (uid < 0) { Log.e(TAG, "Cannot find UID for $pkg"); interrupt(); return }
            val config = configBuilder.addMatchingUid(uid).build()
            val audioFormat = AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
            mCapture = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build AudioRecord for $pkg: ${e.message}")
            interrupt(); return
        }
        try {
            mCapture.startRecording()
            while (playback) {
                val read = mCapture.read(dataBuffer, 0, bufSize)
                if (read > 0) {
                    mPlayers.values.toList().forEach { it.write(dataBuffer, 0, read) }
                }
                loadedCycles++
            }
        } catch (e: Exception) {
            Log.e(TAG, "PlayBackThread error for $pkg: ${e.message}")
        }
    }

    fun createOutput(
        device: AudioDeviceInfo? = null,
        outputKey: Int = device?.id ?: -1,
        startVolume: Float = 100f,
        balance: Float = 0f,
        eqLow: Float = 50f,
        eqMid: Float = 50f,
        eqHigh: Float = 50f,
    ) {
        val player = AccuAudioPlayer(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channel,
            encoding,
            bufSize,
            AudioTrack.MODE_STREAM,
        )
        player.playbackRate = sampleRate
        player.preferredDevice = device
        player.play()
        player.setCurrentVolume(startVolume)
        player.setBalance(balance)
        player.setBand(0, eqLow)
        player.setBand(1, eqMid)
        player.setBand(2, eqHigh)
        mPlayers[outputKey] = player
    }

    fun deleteOutput(outputKey: Int, interrupt: Boolean = true): AccuAudioPlayer? {
        val player = mPlayers.remove(outputKey)
        player?.stop()
        player?.releaseEffects()
        if (mPlayers.isEmpty() && interrupt) interrupt()
        return player
    }

    fun hasOutput(deviceId: Int) = mPlayers.containsKey(deviceId)

    fun setVolume(outputId: Int, vol: Float) = mPlayers[outputId]?.setCurrentVolume(vol)
    fun getVolume(outputId: Int) = mPlayers[outputId]?.volume?.times(100f) ?: 100f
    fun setBalance(outputId: Int, value: Float) = mPlayers[outputId]?.setBalance(value)
    fun getBalance(outputId: Int) = mPlayers[outputId]?.getBalance() ?: 0f
    fun setBand(outputId: Int, band: Int, value: Float) = mPlayers[outputId]?.setBand(band, value)
    fun getBand(outputId: Int, band: Int) = mPlayers[outputId]?.savedBands?.getOrNull(band) ?: 50f

    fun switchDevice(outputKey: Int, newDevice: AudioDeviceInfo?): Boolean {
        val newKey = newDevice?.id ?: -1
        if (mPlayers.containsKey(newKey)) return false
        val old = deleteOutput(outputKey, false) ?: return false
        createOutput(
            device = newDevice,
            outputKey = newKey,
            startVolume = old.volume * 100f,
            balance = old.getBalance(),
            eqLow = old.savedBands.getOrElse(0) { 50f },
            eqMid = old.savedBands.getOrElse(1) { 50f },
            eqHigh = old.savedBands.getOrElse(2) { 50f },
        )
        return true
    }

    fun getLatency(): Float = 30000f / loadedCycles.coerceAtLeast(1).also { loadedCycles = 0 }

    fun calculateRMS(): Float {
        val shortBuf = ShortArray(dataBuffer.size / 2)
        ByteBuffer.wrap(dataBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuf)
        var sum = 0.0
        shortBuf.forEach { sum += it.toLong() * it.toLong() }
        return sqrt(sum / shortBuf.size).toFloat()
    }

    override fun interrupt() {
        playback = false
        try { mCapture.stop(); mCapture.release() } catch (_: Exception) {}
        mPlayers.values.forEach { it.stop(); it.releaseEffects() }
        mPlayers.clear()
        super.interrupt()
    }

    companion object {
        const val TAG = "AccuPlayBack"

        fun getAppUid(context: Context, pkg: String): Int = try {
            context.packageManager.getApplicationInfo(pkg, 0).uid
        } catch (_: Exception) { -1 }
    }
}
