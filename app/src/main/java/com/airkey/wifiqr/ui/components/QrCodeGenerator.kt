package com.airkey.wifiqr.ui.components

import android.graphics.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.airkey.wifiqr.viewmodel.QrStyle
import kotlin.math.*

object QrCodeGenerator {

    fun generate(
        content: String,
        size: Int = 512,
        style: QrStyle = QrStyle(),
        logoBitmap: Bitmap? = null
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawBackground(canvas, size, style)

            val fgColor = style.foregroundColor.toInt()
            val accentColor = style.accentColor.toInt()
            val scale = bitMatrix.width

            for (x in 0 until scale) {
                for (y in 0 until scale) {
                    if (bitMatrix[x, y]) {
                        val cellSize = size.toFloat() / scale
                        val cx = x * cellSize + cellSize / 2
                        val cy = y * cellSize + cellSize / 2
                        drawDot(canvas, cx, cy, cellSize, x, y, scale, style, fgColor, accentColor)
                    }
                }
            }

            drawFrame(canvas, size, style)

            if (style.showLogo) {
                drawLogoCenter(canvas, size, style, logoBitmap)
            }

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun drawBackground(canvas: Canvas, size: Int, style: QrStyle) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (style.frameStyle) {
            1 -> {
                paint.color = 0xFF0D0D1A.toInt()
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                paint.shader = RadialGradient(
                    size / 2f, size / 2f, size * 0.7f,
                    intArrayOf(0x226C63FF.toInt(), 0x1100F5FF.toInt(), 0x00000000.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                paint.shader = null
            }
            2 -> {
                paint.color = 0xFF050510.toInt()
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            }
            3 -> {
                val shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(0xFF0A001A.toInt(), 0xFF001A0A.toInt()),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                paint.shader = null
            }
            else -> {
                paint.color = style.backgroundColor.toInt()
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            }
        }
    }

    private fun drawDot(
        canvas: Canvas, cx: Float, cy: Float, cellSize: Float,
        x: Int, y: Int, scale: Int, style: QrStyle,
        fgColor: Int, accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = cellSize * 0.42f

        val isFinderPattern = (x < 8 && y < 8) || (x >= scale - 8 && y < 8) || (x < 8 && y >= scale - 8)

        val color = when (style.patternIndex) {
            1 -> {
                val ratio = (x.toFloat() / scale + y.toFloat() / scale) / 2f
                interpolateColor(fgColor, accentColor, ratio)
            }
            2 -> {
                val dist = sqrt(((x - scale / 2f).pow(2) + (y - scale / 2f).pow(2))) / (scale / 2f)
                interpolateColor(fgColor, accentColor, dist.coerceIn(0f, 1f))
            }
            3 -> {
                val ratio = x.toFloat() / scale
                interpolateColor(fgColor, accentColor, ratio)
            }
            else -> fgColor
        }

        paint.color = color

        if (style.frameStyle == 1 || style.frameStyle == 2) {
            paint.setShadowLayer(r * 0.8f, 0f, 0f, color)
        }

        if (isFinderPattern) {
            val rect = RectF(cx - r + 1f, cy - r + 1f, cx + r - 1f, cy + r - 1f)
            canvas.drawRoundRect(rect, r * 0.3f, r * 0.3f, paint)
        } else {
            when (style.dotShape) {
                0 -> {
                    val rect = RectF(cx - r, cy - r, cx + r, cy + r)
                    canvas.drawRoundRect(rect, r * 0.5f, r * 0.5f, paint)
                }
                1 -> canvas.drawCircle(cx, cy, r * 0.9f, paint)
                2 -> {
                    val path = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r, cy)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                3 -> canvas.drawPath(starPath(cx, cy, r * 0.95f, r * 0.45f, 5), paint)
                4 -> canvas.drawPath(heartPath(cx, cy, r * 0.9f), paint)
                else -> canvas.drawCircle(cx, cy, r, paint)
            }
        }
    }

    private fun drawFrame(canvas: Canvas, size: Int, style: QrStyle) {
        val accentColor = style.accentColor.toInt()
        val fgColor = style.foregroundColor.toInt()
        val margin = 8f
        val cornerRadius = 24f

        when (style.frameStyle) {
            1 -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(80, 255, 255, 255)
                    this.style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                canvas.drawRoundRect(
                    RectF(margin, margin, size - margin, size - margin),
                    cornerRadius, cornerRadius, paint
                )
            }
            2 -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    this.style = Paint.Style.STROKE
                    strokeWidth = 3f
                    setShadowLayer(12f, 0f, 0f, accentColor)
                }
                canvas.drawRoundRect(
                    RectF(margin, margin, size - margin, size - margin),
                    cornerRadius, cornerRadius, paint
                )
                paint.strokeWidth = 1f
                paint.setShadowLayer(6f, 0f, 0f, fgColor)
                paint.color = fgColor
                canvas.drawRoundRect(
                    RectF(margin + 6, margin + 6, size - margin - 6, size - margin - 6),
                    cornerRadius - 4, cornerRadius - 4, paint
                )
            }
            3 -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    this.style = Paint.Style.STROKE
                    strokeWidth = 4f
                    setShadowLayer(8f, 0f, 0f, accentColor)
                }
                val len = 40f
                canvas.drawLine(margin, margin, margin + len, margin, paint)
                canvas.drawLine(margin, margin, margin, margin + len, paint)
                canvas.drawLine(size - margin - len, margin, size - margin, margin, paint)
                canvas.drawLine(size - margin, margin, size - margin, margin + len, paint)
                canvas.drawLine(margin, size - margin, margin + len, size - margin, paint)
                canvas.drawLine(margin, size - margin - len, margin, size - margin, paint)
                canvas.drawLine(size - margin - len, size - margin, size - margin, size - margin, paint)
                canvas.drawLine(size - margin, size - margin - len, size - margin, size - margin, paint)
            }
            else -> {}
        }
    }

    private fun drawLogoCenter(canvas: Canvas, size: Int, style: QrStyle, logoBitmap: Bitmap? = null) {
        val logoSize = size * 0.20f
        val cx = size / 2f
        val cy = size / 2f
        val r = logoSize / 2
        val cornerRadius = r * 0.40f          // rounded corners, not full circle
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background rounded rect with glow
        paint.color = style.backgroundColor.toInt()
        paint.setShadowLayer(10f, 0f, 0f, style.accentColor.toInt())
        canvas.drawRoundRect(
            RectF(cx - r - 7f, cy - r - 7f, cx + r + 7f, cy + r + 7f),
            cornerRadius + 4f, cornerRadius + 4f, paint
        )
        paint.clearShadowLayer()

        if (logoBitmap != null) {
            val saveCount = canvas.save()
            val clipPath = Path()
            clipPath.addRoundRect(
                RectF(cx - r, cy - r, cx + r, cy + r),
                cornerRadius, cornerRadius,
                Path.Direction.CW
            )
            canvas.clipPath(clipPath)
            // scale the source bitmap into the destination rect
            val src = android.graphics.Rect(0, 0, logoBitmap.width, logoBitmap.height)
            canvas.drawBitmap(logoBitmap, src, RectF(cx - r, cy - r, cx + r, cy + r), paint)
            canvas.restoreToCount(saveCount)
        } else {
            paint.shader = LinearGradient(
                cx - logoSize / 2, cy - logoSize / 2,
                cx + logoSize / 2, cy + logoSize / 2,
                intArrayOf(style.foregroundColor.toInt(), style.accentColor.toInt()),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, logoSize / 2, paint)
            paint.shader = null

            paint.color = Color.WHITE
            paint.textSize = logoSize * 0.42f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textY = cy - (paint.descent() + paint.ascent()) / 2
            canvas.drawText(style.logoText.take(2).uppercase(), cx, textY, paint)
        }
    }

    private fun starPath(cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int): Path {
        val path = Path()
        val step = Math.PI / points
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = i * step - Math.PI / 2
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun heartPath(cx: Float, cy: Float, size: Float): Path {
        val path = Path()
        val s = size * 0.85f
        path.moveTo(cx, cy + s * 0.35f)
        path.cubicTo(cx - s, cy - s * 0.1f, cx - s * 1.2f, cy - s * 0.9f, cx, cy - s * 0.3f)
        path.cubicTo(cx + s * 1.2f, cy - s * 0.9f, cx + s, cy - s * 0.1f, cx, cy + s * 0.35f)
        path.close()
        return path
    }

    private fun interpolateColor(c1: Int, c2: Int, ratio: Float): Int {
        val r = (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * ratio).toInt().coerceIn(0, 255)
        val a = (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * ratio).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
