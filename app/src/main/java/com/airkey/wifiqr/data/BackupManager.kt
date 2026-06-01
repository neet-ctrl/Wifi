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

object BackupManager {

    private const val BACKUP_VERSION = 1

    fun performBackup(context: Context, folderUri: Uri, networks: List<WifiNetwork>): Result<String> {
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

            val json = buildBackupJson(networks)
            context.contentResolver.openOutputStream(fileUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: return Result.failure(Exception("Could not write to backup file"))

            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun performRestore(context: Context, fileUri: Uri): Result<List<WifiNetwork>> {
        return try {
            val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Cannot open backup file"))
            val json = bytes.toString(Charsets.UTF_8)
            val networks = parseBackupJson(json)
            Result.success(networks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildBackupJson(networks: List<WifiNetwork>): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("appName", "AirKey")
        root.put("backupDate", System.currentTimeMillis())
        root.put("networkCount", networks.size)

        val arr = JSONArray()
        val defaultStyle = QrStyle()

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
                val bmp = QrCodeGenerator.generate(n.toWifiQrString(), 512, defaultStyle, null)
                if (bmp != null) {
                    val bos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
                    obj.put("qrImageBase64", Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP))
                }
            } catch (_: Exception) {}

            arr.put(obj)
        }

        root.put("networks", arr)
        return root.toString(2)
    }

    private fun parseBackupJson(json: String): List<WifiNetwork> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("networks") ?: return emptyList()
        val result = mutableListOf<WifiNetwork>()

        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
                result.add(
                    WifiNetwork(
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
                )
            } catch (_: Exception) {}
        }
        return result
    }
}
