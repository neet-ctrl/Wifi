package com.airkey.wifiqr.ui.screens

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.airkey.wifiqr.data.WifiNetwork
import com.airkey.wifiqr.ui.theme.*
import java.nio.charset.Charset

enum class NfcWriteState {
    WAITING,
    TAG_DETECTED,
    WRITING,
    SUCCESS,
    VERIFIED,
    ERROR,
    NO_NFC,
    ERASING,
    ERASED
}

@Composable
fun NfcWriterScreen(
    network: WifiNetwork,
    discoveredTag: Tag?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(NfcWriteState.WAITING) }
    var errorMsg by remember { mutableStateOf("") }
    var tagInfo by remember { mutableStateOf("") }
    var currentTag by remember { mutableStateOf<Tag?>(null) }

    val nfcAdapter = remember {
        NfcAdapter.getDefaultAdapter(context)
    }

    LaunchedEffect(Unit) {
        if (nfcAdapter == null) {
            state = NfcWriteState.NO_NFC
        }
    }

    LaunchedEffect(discoveredTag) {
        if (discoveredTag != null && state != NfcWriteState.NO_NFC) {
            currentTag = discoveredTag
            val ndef = Ndef.get(discoveredTag)
            tagInfo = if (ndef != null) {
                "Type: ${ndef.type}  ·  Size: ${ndef.maxSize} bytes  ·  ${if (ndef.isWritable) "Writable" else "Read-only"}"
            } else {
                val formatable = NdefFormatable.get(discoveredTag)
                if (formatable != null) "Unformatted tag — will format on write" else "Tag type: ${discoveredTag.techList?.firstOrNull() ?: "Unknown"}"
            }
            state = NfcWriteState.TAG_DETECTED
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "nfc")
    val ring1 by infiniteTransition.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "r1"
    )
    val ring2 by infiniteTransition.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1200, 400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "r2"
    )
    val ring3 by infiniteTransition.animateFloat(
        0.9f, 1f,
        infiniteRepeatable(tween(1200, 800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "r3"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(GlassWhite, CircleShape)) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NFC Tag Writer", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(network.ssid, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                if (state == NfcWriteState.WAITING) {
                    for ((i, alpha) in listOf(ring1, ring2, ring3).withIndex()) {
                        val size = (100 + i * 36).dp
                        Box(
                            modifier = Modifier
                                .size(size)
                                .alpha(alpha * 0.4f)
                                .border(2.dp, NeonCyan.copy(alpha), CircleShape)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .coloredShadow(
                            when (state) {
                                NfcWriteState.SUCCESS, NfcWriteState.VERIFIED -> GreenSuccess
                                NfcWriteState.ERROR -> RedError
                                NfcWriteState.TAG_DETECTED -> NeonPurple
                                else -> NeonCyan
                            }, 40.dp, 30.dp, alpha = 0.6f
                        )
                        .background(
                            Brush.linearGradient(
                                when (state) {
                                    NfcWriteState.SUCCESS, NfcWriteState.VERIFIED -> listOf(GreenSuccess.copy(0.3f), GreenSuccess.copy(0.1f))
                                    NfcWriteState.ERROR -> listOf(RedError.copy(0.3f), RedError.copy(0.1f))
                                    NfcWriteState.TAG_DETECTED -> listOf(NeonPurple.copy(0.3f), NeonCyan.copy(0.2f))
                                    else -> listOf(NeonCyan.copy(0.25f), NeonPurple.copy(0.15f))
                                }
                            ),
                            CircleShape
                        )
                        .border(2.dp, when (state) {
                            NfcWriteState.SUCCESS, NfcWriteState.VERIFIED -> GreenSuccess.copy(0.6f)
                            NfcWriteState.ERROR -> RedError.copy(0.6f)
                            else -> NeonCyan.copy(0.5f)
                        }, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        NfcWriteState.WRITING, NfcWriteState.ERASING -> CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                        NfcWriteState.SUCCESS, NfcWriteState.VERIFIED -> Icon(Icons.Rounded.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(44.dp))
                        NfcWriteState.ERROR -> Icon(Icons.Rounded.ErrorOutline, null, tint = RedError, modifier = Modifier.size(44.dp))
                        NfcWriteState.NO_NFC -> Icon(Icons.Rounded.Block, null, tint = RedError, modifier = Modifier.size(44.dp))
                        NfcWriteState.ERASED -> Icon(Icons.Rounded.LayersClear, null, tint = OrangeWarn, modifier = Modifier.size(44.dp))
                        else -> Icon(Icons.Rounded.Nfc, null, tint = NeonCyan, modifier = Modifier.size(44.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                when (state) {
                    NfcWriteState.WAITING -> "Hold Phone to NFC Tag"
                    NfcWriteState.TAG_DETECTED -> "Tag Detected!"
                    NfcWriteState.WRITING -> "Writing WiFi data…"
                    NfcWriteState.SUCCESS -> "Written Successfully!"
                    NfcWriteState.VERIFIED -> "Verified & Ready"
                    NfcWriteState.ERROR -> "Write Failed"
                    NfcWriteState.NO_NFC -> "NFC Not Available"
                    NfcWriteState.ERASING -> "Erasing Tag…"
                    NfcWriteState.ERASED -> "Tag Erased"
                },
                style = MaterialTheme.typography.titleLarge,
                color = when (state) {
                    NfcWriteState.SUCCESS, NfcWriteState.VERIFIED -> GreenSuccess
                    NfcWriteState.ERROR, NfcWriteState.NO_NFC -> RedError
                    NfcWriteState.TAG_DETECTED -> NeonPurple
                    else -> Color.White
                },
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                when (state) {
                    NfcWriteState.WAITING -> "Place an NFC sticker or card near the top/back of your phone. The app will detect it automatically."
                    NfcWriteState.TAG_DETECTED -> tagInfo
                    NfcWriteState.WRITING -> "Keep the tag in contact with your phone…"
                    NfcWriteState.SUCCESS -> "Anyone who taps this tag with their phone will be instantly prompted to join ${network.ssid}."
                    NfcWriteState.VERIFIED -> "Tag verified — WiFi data matches. Ready to use!"
                    NfcWriteState.ERROR -> errorMsg
                    NfcWriteState.NO_NFC -> "This device does not have an NFC chip, or NFC is turned off in Settings."
                    NfcWriteState.ERASING -> "Clearing tag data…"
                    NfcWriteState.ERASED -> "The NFC tag has been erased and is blank again."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            if (state == NfcWriteState.TAG_DETECTED || state == NfcWriteState.SUCCESS || state == NfcWriteState.ERROR || state == NfcWriteState.ERASED) {
                Button(
                    onClick = {
                        val tag = currentTag ?: return@Button
                        state = NfcWriteState.WRITING
                        val writeResult = writeNdefWifi(tag, network.toWifiQrString())
                        state = if (writeResult) NfcWriteState.SUCCESS else {
                            errorMsg = "Could not write to tag. Make sure the tag is writable and in contact."
                            NfcWriteState.ERROR
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Nfc, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Write WiFi to Tag", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (state == NfcWriteState.SUCCESS) {
                    Button(
                        onClick = {
                            val tag = currentTag ?: return@Button
                            val verified = verifyNdefTag(tag, network.toWifiQrString())
                            state = if (verified) NfcWriteState.VERIFIED else {
                                errorMsg = "Data mismatch — tag content does not match expected WiFi string."
                                NfcWriteState.ERROR
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess.copy(0.15f), contentColor = GreenSuccess),
                        border = BorderStroke(1.dp, GreenSuccess.copy(0.5f))
                    ) {
                        Icon(Icons.Rounded.FactCheck, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Verify Tag Contents", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedButton(
                    onClick = {
                        val tag = currentTag ?: return@OutlinedButton
                        state = NfcWriteState.ERASING
                        val erased = eraseNdefTag(tag)
                        state = if (erased) NfcWriteState.ERASED else {
                            errorMsg = "Could not erase tag."
                            NfcWriteState.ERROR
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RedError.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
                ) {
                    Icon(Icons.Rounded.LayersClear, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Erase Tag")
                }
            }

            if (state == NfcWriteState.SUCCESS || state == NfcWriteState.VERIFIED) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { state = NfcWriteState.WAITING; currentTag = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeonPurple.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Write Another Tag")
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassWhite, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassWhite2, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("What is NFC WiFi?", style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Text("Write your WiFi credentials to a cheap NFC sticker (< \$1 each). Stick it anywhere — fridge, table, sign. When guests tap their phone on the sticker, Android/iOS automatically prompts them to join your network. No scanning, no typing.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Divider(color = GlassWhite2)
                NfcInfoRow(Icons.Rounded.Wifi, "Works with", "Any NFC-enabled Android phone (Android 4.0+) and iPhone 7+ with iOS 14+")
                NfcInfoRow(Icons.Rounded.ShoppingCart, "Tag type", "NTAG213 or NTAG215 stickers recommended (most common & cheapest)")
                NfcInfoRow(Icons.Rounded.Lock, "Security", "Credentials are stored on the tag — anyone who taps it can join the network")
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun NfcInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = NeonPurple, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

fun writeNdefWifi(tag: Tag, wifiQrString: String): Boolean {
    return try {
        val payload = wifiQrString.toByteArray(Charset.forName("UTF-8"))
        val record = NdefRecord.createTextRecord("en", wifiQrString)
        val message = NdefMessage(arrayOf(record))

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            if (!ndef.isWritable) { ndef.close(); return false }
            ndef.writeNdefMessage(message)
            ndef.close()
            true
        } else {
            val formatable = NdefFormatable.get(tag)
            if (formatable != null) {
                formatable.connect()
                formatable.format(message)
                formatable.close()
                true
            } else false
        }
    } catch (_: Exception) { false }
}

fun verifyNdefTag(tag: Tag, expected: String): Boolean {
    return try {
        val ndef = Ndef.get(tag) ?: return false
        ndef.connect()
        val msg = ndef.ndefMessage
        ndef.close()
        val record = msg?.records?.firstOrNull() ?: return false
        val payload = record.payload
        val langLen = payload[0].toInt() and 0x3F
        val text = String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
        text.trim() == expected.trim()
    } catch (_: Exception) { false }
}

fun eraseNdefTag(tag: Tag): Boolean {
    return try {
        val empty = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", "")))
        val ndef = Ndef.get(tag) ?: return false
        ndef.connect()
        ndef.writeNdefMessage(empty)
        ndef.close()
        true
    } catch (_: Exception) { false }
}
