package com.airkey.wifiqr.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import com.airkey.wifiqr.ui.components.QrCodeGenerator
import com.airkey.wifiqr.viewmodel.QrStyle
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreResult(
    val networks: List<Pair<WifiNetwork, ByteArray?>>,
    val connectionEvents: List<ConnectionEvent>,
    val geofenceConfigs: List<GeofenceConfig>
)

object BackupManager {

    private const val BACKUP_VERSION = 2

    fun performBackup(
        context: Context,
        folderUri: Uri,
        networks: List<WifiNetwork>,
        events: List<ConnectionEvent> = emptyList(),
        geofences: List<GeofenceConfig> = emptyList()
    ): Result<String> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "AirKey_Backup_$timestamp.wifi"

            val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocId)
            val fileUri = DocumentsContract.createDocument(
                context.contentResolver,
                docUri,
                "application/octet-stream",
                fileName
            ) ?: return Result.failure(Exception("Could not create backup file in the chosen folder"))

            val json = buildBackupJson(networks, events, geofences)
            context.contentResolver.openOutputStream(fileUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: return Result.failure(Exception("Could not write to backup file"))

            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun performRestore(context: Context, fileUri: Uri): Result<RestoreResult> {
        return try {
            val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Cannot open backup file"))
            val json = bytes.toString(Charsets.UTF_8)
            Result.success(parseBackupJson(json))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildBackupJson(
        networks: List<WifiNetwork>,
        events: List<ConnectionEvent>,
        geofences: List<GeofenceConfig>
    ): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("appName", "AirKey")
        root.put("backupDate", System.currentTimeMillis())
        root.put("networkCount", networks.size)

        // ── Networks + QR images ────────────────────────────────────────────
        val defaultStyle = QrStyle()
        val networksArr = JSONArray()
        for (n in networks) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("ssid", n.ssid)
            obj.put("password", n.password)
            obj.put("securityType", n.securityType)
            obj.put("isHidden", n.isHidden)
            obj.put("notes", n.notes)
            obj.put("savedAt", n.savedAt)
            if (n.lastConnected != null) obj.put("lastConnected", n.lastConnected) else obj.put("lastConnected", JSONObject.NULL)
            obj.put("category", n.category)
            obj.put("isFavorite", n.isFavorite)
            try {
                val bmp: Bitmap? = n.qrCodeImagePath?.let { QrImageStore.load(it) }
                    ?: QrCodeGenerator.generate(n.toWifiQrString(), 512, defaultStyle, null)
                if (bmp != null) {
                    val bos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
                    obj.put("qrImageBase64", Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP))
                }
            } catch (_: Exception) {}
            networksArr.put(obj)
        }
        root.put("networks", networksArr)

        // ── Connection events (History + Speed Test data) ───────────────────
        val eventsArr = JSONArray()
        for (e in events) {
            val obj = JSONObject()
            obj.put("networkId", e.networkId)
            obj.put("ssid", e.ssid)
            obj.put("connectedAt", e.connectedAt)
            if (e.downloadSpeedMbps != null) obj.put("downloadSpeedMbps", e.downloadSpeedMbps) else obj.put("downloadSpeedMbps", JSONObject.NULL)
            if (e.uploadSpeedMbps != null) obj.put("uploadSpeedMbps", e.uploadSpeedMbps) else obj.put("uploadSpeedMbps", JSONObject.NULL)
            if (e.pingMs != null) obj.put("pingMs", e.pingMs) else obj.put("pingMs", JSONObject.NULL)
            if (e.signalDbm != null) obj.put("signalDbm", e.signalDbm) else obj.put("signalDbm", JSONObject.NULL)
            if (e.frequencyMhz != null) obj.put("frequencyMhz", e.frequencyMhz) else obj.put("frequencyMhz", JSONObject.NULL)
            if (e.linkSpeedMbps != null) obj.put("linkSpeedMbps", e.linkSpeedMbps) else obj.put("linkSpeedMbps", JSONObject.NULL)
            eventsArr.put(obj)
        }
        root.put("connectionEvents", eventsArr)

        // ── Geofence configs ────────────────────────────────────────────────
        val geofencesArr = JSONArray()
        for (g in geofences) {
            val obj = JSONObject()
            obj.put("networkId", g.networkId)
            obj.put("latitude", g.latitude)
            obj.put("longitude", g.longitude)
            obj.put("radiusMeters", g.radiusMeters)
            obj.put("enabled", g.enabled)
            obj.put("label", g.label)
            geofencesArr.put(obj)
        }
        root.put("geofenceConfigs", geofencesArr)

        return root.toString(2)
    }

    private fun parseBackupJson(json: String): RestoreResult {
        val root = JSONObject(json)

        // ── Networks ────────────────────────────────────────────────────────
        val networksArr = root.optJSONArray("networks") ?: JSONArray()
        val networks = mutableListOf<Pair<WifiNetwork, ByteArray?>>()
        for (i in 0 until networksArr.length()) {
            try {
                val obj = networksArr.getJSONObject(i)
                val network = WifiNetwork(
                    id = 0,
                    ssid = obj.getString("ssid"),
                    password = obj.optString("password", ""),
                    securityType = obj.optString("securityType", "WPA"),
                    isHidden = obj.optBoolean("isHidden", false),
                    notes = obj.optString("notes", ""),
                    savedAt = obj.optLong("savedAt", System.currentTimeMillis()),
                    lastConnected = if (obj.isNull("lastConnected")) null else obj.optLong("lastConnected"),
                    category = obj.optString("category", "General"),
                    isFavorite = obj.optBoolean("isFavorite", false)
                )
                val imageBytes: ByteArray? = obj.optString("qrImageBase64", "")
                    .takeIf { it.isNotEmpty() }
                    ?.let { try { Base64.decode(it, Base64.NO_WRAP) } catch (_: Exception) { null } }
                networks.add(Pair(network, imageBytes))
            } catch (_: Exception) {}
        }

        // ── Connection events ───────────────────────────────────────────────
        val eventsArr = root.optJSONArray("connectionEvents") ?: JSONArray()
        val events = mutableListOf<ConnectionEvent>()
        for (i in 0 until eventsArr.length()) {
            try {
                val obj = eventsArr.getJSONObject(i)
                events.add(ConnectionEvent(
                    id = 0,
                    networkId = obj.getLong("networkId"),
                    ssid = obj.getString("ssid"),
                    connectedAt = obj.getLong("connectedAt"),
                    downloadSpeedMbps = if (obj.isNull("downloadSpeedMbps")) null else obj.optDouble("downloadSpeedMbps").toFloat(),
                    uploadSpeedMbps = if (obj.isNull("uploadSpeedMbps")) null else obj.optDouble("uploadSpeedMbps").toFloat(),
                    pingMs = if (obj.isNull("pingMs")) null else obj.optInt("pingMs"),
                    signalDbm = if (obj.isNull("signalDbm")) null else obj.optInt("signalDbm"),
                    frequencyMhz = if (obj.isNull("frequencyMhz")) null else obj.optInt("frequencyMhz"),
                    linkSpeedMbps = if (obj.isNull("linkSpeedMbps")) null else obj.optInt("linkSpeedMbps")
                ))
            } catch (_: Exception) {}
        }

        // ── Geofence configs ────────────────────────────────────────────────
        val geofencesArr = root.optJSONArray("geofenceConfigs") ?: JSONArray()
        val geofences = mutableListOf<GeofenceConfig>()
        for (i in 0 until geofencesArr.length()) {
            try {
                val obj = geofencesArr.getJSONObject(i)
                geofences.add(GeofenceConfig(
                    networkId = obj.getLong("networkId"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    radiusMeters = obj.optDouble("radiusMeters", 100.0).toFloat(),
                    enabled = obj.optBoolean("enabled", true),
                    label = obj.optString("label", "")
                ))
            } catch (_: Exception) {}
        }

        return RestoreResult(networks, events, geofences)
    }
}
