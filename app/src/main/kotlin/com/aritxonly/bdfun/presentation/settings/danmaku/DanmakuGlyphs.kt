package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.settings.application.*

internal enum class DmIconKind { Fixed, Scroll, Color, Advanced, Count }

@Composable
internal fun DmTypeGlyph(kind: DmIconKind, tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        when (kind) {
            DmIconKind.Fixed -> drawDmFixed(tint)
            DmIconKind.Scroll -> drawDmScroll(tint)
            DmIconKind.Color -> drawDmColor(tint)
            DmIconKind.Advanced -> drawDmAdvanced(tint)
            DmIconKind.Count -> drawDmCount(tint)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDmFixed(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.12f, h * 0.28f),
        size = Size(w * 0.76f, h * 0.44f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(stroke),
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.30f, h * 0.5f),
        end = Offset(w * 0.70f, h * 0.5f),
        strokeWidth = stroke,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDmScroll(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    drawLine(
        color = tint,
        start = Offset(w * 0.12f, h * 0.38f),
        end = Offset(w * 0.86f, h * 0.38f),
        strokeWidth = stroke,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.86f, h * 0.38f),
        end = Offset(w * 0.68f, h * 0.20f),
        strokeWidth = stroke,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.14f, h * 0.66f),
        end = Offset(w * 0.88f, h * 0.66f),
        strokeWidth = stroke,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.14f, h * 0.66f),
        end = Offset(w * 0.32f, h * 0.84f),
        strokeWidth = stroke,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDmColor(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    drawCircle(
        color = tint,
        radius = w * 0.34f,
        center = Offset(w * 0.46f, h * 0.52f),
        style = Stroke(stroke),
    )
    drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.34f, h * 0.42f))
    drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.52f, h * 0.34f))
    drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.61f, h * 0.48f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDmAdvanced(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    val cx = w * 0.58f
    val cy = h * 0.42f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - h * 0.18f)
        lineTo(cx + w * 0.06f, cy - h * 0.03f)
        lineTo(cx + w * 0.21f, cy)
        lineTo(cx + w * 0.06f, cy + h * 0.03f)
        lineTo(cx, cy + h * 0.18f)
        lineTo(cx - w * 0.06f, cy + h * 0.03f)
        lineTo(cx - w * 0.21f, cy)
        lineTo(cx - w * 0.06f, cy - h * 0.03f)
        close()
    }
    drawPath(path, tint)
    drawLine(
        color = tint,
        start = Offset(w * 0.12f, h * 0.50f),
        end = Offset(w * 0.28f, h * 0.50f),
        strokeWidth = stroke,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDmCount(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = tint.toArgb()
            textSize = 10.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.nativeCanvas.drawText("×99", w / 2f, h * 0.62f, paint)
    }
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.06f, h * 0.24f),
        size = Size(w * 0.88f, h * 0.52f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(stroke * 0.8f),
    )
}
