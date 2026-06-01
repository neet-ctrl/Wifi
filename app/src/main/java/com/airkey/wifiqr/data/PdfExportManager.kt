package com.airkey.wifiqr.data

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.DocumentsContract
import com.airkey.wifiqr.ui.components.QrCodeGenerator
import com.airkey.wifiqr.viewmodel.QrStyle
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object PdfExportManager {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun exportBooklet(
        context: Context,
        networks: List<WifiNetwork>,
        folderUri: Uri
    ): Result<String> {
        return try {
            val fileName = "AirKey_QR_Booklet_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            }.pdf"

            val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocId)
            val fileUri = DocumentsContract.createDocument(
                context.contentResolver,
                docUri,
                "application/pdf",
                fileName
            ) ?: return Result.failure(IOException("Cannot create PDF file in selected folder"))

            val pdf = PdfDocument()

            networks.forEachIndexed { index, network ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                val page = pdf.startPage(pageInfo)
                drawNetworkPage(page.canvas, network, index + 1, networks.size)
                pdf.finishPage(page)
            }

            context.contentResolver.openOutputStream(fileUri)?.use { out ->
                pdf.writeTo(out)
            } ?: return Result.failure(IOException("Cannot write PDF"))

            pdf.close()
            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawNetworkPage(
        canvas: Canvas,
        network: WifiNetwork,
        pageNum: Int,
        total: Int
    ) {
        val w = 595f
        val h = 842f

        val bgPaint = Paint().apply { color = Color.parseColor("#0A0A1A") }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val gradPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w, 200f,
                intArrayOf(Color.parseColor("#6C63FF"), Color.parseColor("#00F5FF")),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, 180f, gradPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AirKey", 40f, 80f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCFFFFFF")
            textSize = 16f
        }
        canvas.drawText("WiFi QR Code Booklet", 40f, 108f, subPaint)

        val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCFFFFFF")
            textSize = 14f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("$pageNum / $total", w - 40f, 108f, pageNumPaint)

        val storedBitmap = network.qrCodeImagePath?.let { QrImageStore.load(it) }
        val qrBitmap = storedBitmap ?: QrCodeGenerator.generate(
            network.toWifiQrString(),
            400,
            QrStyle(
                patternIndex = 1,
                foregroundColor = 0xFF6C63FF,
                backgroundColor = 0xFF0A0A1A,
                accentColor = 0xFF00F5FF
            ),
            null
        )

        if (qrBitmap != null) {
            val qrSize = 280f
            val qrLeft = (w - qrSize) / 2f
            val qrTop = 200f
            val dst = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)

            val qrBgPaint = Paint().apply { color = Color.parseColor("#12122A") }
            canvas.drawRoundRect(
                RectF(qrLeft - 16f, qrTop - 16f, qrLeft + qrSize + 16f, qrTop + qrSize + 16f),
                24f, 24f, qrBgPaint
            )
            canvas.drawBitmap(qrBitmap, null, dst, null)
        }

        val labelTop = 520f

        val ssidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            if (network.ssid.length > 30) network.ssid.take(28) + "…" else network.ssid,
            w / 2f, labelTop, ssidPaint
        )

        val divPaint = Paint().apply {
            color = Color.parseColor("#336C63FF")
            strokeWidth = 1f
        }
        canvas.drawLine(60f, labelTop + 20f, w - 60f, labelTop + 20f, divPaint)

        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAFFFFFF")
            textSize = 15f
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val rows = buildList {
            add("Security" to (try { SecurityType.valueOf(network.securityType).display } catch (_: Exception) { network.securityType }))
            if (network.password.isNotEmpty()) add("Password" to network.password)
            if (network.category != "General") add("Category" to network.category)
            if (network.isHidden) add("Type" to "Hidden Network")
            add("Saved on" to dateFormat.format(Date(network.savedAt)))
        }

        rows.forEachIndexed { i, (label, value) ->
            val y = labelTop + 55f + i * 32f
            canvas.drawText(label, 60f, y, rowPaint)
            val displayVal = if (label == "Password") "●".repeat(value.length.coerceAtMost(16)) else value
            canvas.drawText(displayVal, w - 60f, y, valPaint)
        }

        if (network.notes.isNotEmpty()) {
            val noteY = labelTop + 55f + rows.size * 32f + 20f
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8800F5FF")
                textSize = 13f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Note: ${network.notes}", w / 2f, noteY, notePaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#556C63FF")
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated by AirKey · Scan QR to connect instantly", w / 2f, h - 30f, footerPaint)
    }
}
