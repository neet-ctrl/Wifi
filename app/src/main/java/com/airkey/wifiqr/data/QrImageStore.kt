package com.airkey.wifiqr.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object QrImageStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "qr_images").also { it.mkdirs() }

    fun save(context: Context, networkId: Long, bitmap: Bitmap): String {
        val file = File(dir(context), "$networkId.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun load(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path)
    } catch (_: Exception) { null }

    fun delete(context: Context, networkId: Long) {
        File(dir(context), "$networkId.png").delete()
    }

    fun saveFromBytes(context: Context, networkId: Long, bytes: ByteArray): String {
        val file = File(dir(context), "$networkId.png")
        file.writeBytes(bytes)
        return file.absolutePath
    }
}
