package com.airkey.wifiqr.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.*

fun Modifier.coloredShadow(
    color: Color,
    borderRadius: Dp = 20.dp,
    blurRadius: Dp = 24.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 8.dp,
    alpha: Float = 0.5f
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.isAntiAlias = true
        frameworkPaint.color = android.graphics.Color.TRANSPARENT
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            color.copy(alpha = alpha).toArgb()
        )
        canvas.drawRoundRect(
            0f, 0f,
            size.width, size.height,
            borderRadius.toPx(), borderRadius.toPx(),
            paint
        )
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var width by remember { mutableIntStateOf(0) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by transition.animateFloat(
        initialValue = -2f * width,
        targetValue = 2f * width,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    this
        .onGloballyPositioned { width = it.size.width }
        .background(
            Brush.linearGradient(
                colors = listOf(CardSurface, Color(0xFF252545), CardSurface),
                start = Offset(offsetX, 0f),
                end = Offset(offsetX + width.toFloat(), width * 0.5f)
            )
        )
}

fun Modifier.tiltOnTouch(maxTiltDeg: Float = 8f): Modifier = composed {
    var rotX by remember { mutableFloatStateOf(0f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    val animRotX by animateFloatAsState(
        targetValue = rotX,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "rotX"
    )
    val animRotY by animateFloatAsState(
        targetValue = rotY,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "rotY"
    )
    this
        .graphicsLayer {
            rotationX = animRotX
            rotationY = animRotY
            cameraDistance = 12f * density
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) {
                        rotX = 0f
                        rotY = 0f
                        break
                    }
                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                    val y = change.position.y.coerceIn(0f, size.height.toFloat())
                    rotX = ((y / size.height) - 0.5f) * -maxTiltDeg * 2f
                    rotY = ((x / size.width) - 0.5f) * maxTiltDeg * 2f
                }
            }
        }
}
