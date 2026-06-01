package com.accu.audio

import android.media.AudioDeviceInfo

data class SoundMasterEntry(
    val pkg: String,
    val outputDeviceId: Int = -1,
    val volume: Float = 100f,
    val balance: Float = 0f,
    val eqLow: Float = 50f,
    val eqMid: Float = 50f,
    val eqHigh: Float = 50f,
    val locked: Boolean = false,
)

data class SoundMasterPreset(
    val name: String,
    val volume: Float,
    val balance: Float,
    val eqLow: Float,
    val eqMid: Float,
    val eqHigh: Float,
)

enum class MixedAudioFocus {
    ALLOWED, IGNORED, DENIED
}

data class MixedAudioAppState(
    val pkg: String,
    val appName: String,
    val muted: Boolean,
    val focus: MixedAudioFocus,
)

fun AudioDeviceInfo?.displayName(): String = when {
    this == null -> "Default Output"
    else -> {
        val type = when (this.type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE    -> "Earpiece"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER     -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET       -> "Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES    -> "Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP      -> "Bluetooth Audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO       -> "Bluetooth Call"
            AudioDeviceInfo.TYPE_HDMI                -> "HDMI"
            AudioDeviceInfo.TYPE_USB_HEADSET         -> "USB Headset"
            AudioDeviceInfo.TYPE_USB_DEVICE          -> "USB Device"
            AudioDeviceInfo.TYPE_USB_ACCESSORY       -> "USB Accessory"
            AudioDeviceInfo.TYPE_DOCK                -> "Dock"
            AudioDeviceInfo.TYPE_AUX_LINE            -> "AUX"
            AudioDeviceInfo.TYPE_BLE_HEADSET         -> "BLE Headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER         -> "BLE Speaker"
            else                                     -> "Device (${this.type})"
        }
        "${this.productName} · $type"
    }
}

val DEFAULT_PRESETS = listOf(
    SoundMasterPreset("Flat",          100f, 0f, 50f, 50f, 50f),
    SoundMasterPreset("Bass Boost",    100f, 0f, 80f, 50f, 40f),
    SoundMasterPreset("Treble Boost",  100f, 0f, 40f, 50f, 80f),
    SoundMasterPreset("V-Shape",       100f, 0f, 75f, 30f, 75f),
    SoundMasterPreset("Vocal",         100f, 0f, 35f, 80f, 60f),
    SoundMasterPreset("Podcast",       100f, 0f, 40f, 75f, 55f),
    SoundMasterPreset("Night Mode",    60f,  0f, 40f, 45f, 35f),
    SoundMasterPreset("Boost+",        150f, 0f, 60f, 55f, 60f),
)
