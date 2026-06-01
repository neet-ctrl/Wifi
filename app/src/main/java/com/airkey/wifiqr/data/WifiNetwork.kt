package com.airkey.wifiqr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SecurityType(val display: String, val wifiCode: String) {
    WPA("WPA/WPA2", "WPA"),
    WPA3("WPA3", "WPA3"),
    WEP("WEP (Legacy)", "WEP"),
    OPEN("Open (No Password)", "nopass")
}

@Entity(tableName = "wifi_networks")
data class WifiNetwork(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val password: String,
    val securityType: String = SecurityType.WPA.name,
    val isHidden: Boolean = false,
    val notes: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val lastConnected: Long? = null,
    val category: String = "General",
    val isFavorite: Boolean = false,
    val qrCodeImagePath: String? = null
) {
    fun toWifiQrString(): String {
        val sec = when (securityType) {
            SecurityType.OPEN.name -> "nopass"
            SecurityType.WEP.name -> "WEP"
            SecurityType.WPA3.name -> "WPA"
            else -> "WPA"
        }
        val escapedSsid = ssid
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\"", "\\\"")
            .replace(":", "\\:")
        val escapedPass = password
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\"", "\\\"")
            .replace(":", "\\:")
        val hidden = if (isHidden) "H:true;" else ""
        return "WIFI:T:$sec;S:$escapedSsid;P:$escapedPass;$hidden;"
    }

    fun getSecurityEnum(): SecurityType {
        return try { SecurityType.valueOf(securityType) } catch (e: Exception) { SecurityType.WPA }
    }
}

data class ScannedWifiResult(
    val ssid: String,
    val password: String,
    val securityType: String,
    val isHidden: Boolean,
    val rawQrContent: String
)

fun parseWifiQrCode(raw: String): ScannedWifiResult? {
    if (!raw.startsWith("WIFI:", ignoreCase = true)) return null
    fun extract(key: String): String {
        val pattern = Regex("(?i)$key:((?:[^;\\\\]|\\\\.)*)(?:;|\$)")
        val match = pattern.find(raw) ?: return ""
        return match.groupValues[1]
            .replace("\\\\", "\\")
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\\"", "\"")
            .replace("\\:", ":")
    }
    val ssid = extract("S")
    if (ssid.isBlank()) return null
    val password = extract("P")
    val secType = extract("T").uppercase()
    val hidden = extract("H").lowercase() == "true"
    val security = when {
        secType == "WEP" -> SecurityType.WEP.name
        secType == "NOPASS" || secType.isEmpty() -> SecurityType.OPEN.name
        secType == "WPA3" -> SecurityType.WPA3.name
        else -> SecurityType.WPA.name
    }
    return ScannedWifiResult(
        ssid = ssid,
        password = password,
        securityType = security,
        isHidden = hidden,
        rawQrContent = raw
    )
}
