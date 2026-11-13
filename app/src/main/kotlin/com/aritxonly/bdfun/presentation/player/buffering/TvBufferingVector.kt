package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

private val TvPink = Color(0xFFF48FB1)

@Composable
internal fun TvBufferingVector(tvSize: Dp, ui: TvBufferingPresentationStateHolder) {
    Canvas(modifier = Modifier.size(tvSize)) {
        val stroke = size.width * 0.07f
        val left = size.width * 0.12f
        val top = size.height * 0.27f
        val right = size.width * 0.88f
        val bottom = size.height * 0.82f
        val radius = size.width * 0.13f

        // 两根圆头天线
        drawLine(
            color = TvPink,
            start = Offset(size.width * 0.35f, size.height * 0.08f),
            end = Offset(size.width * 0.5f, size.height * 0.27f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = TvPink,
            start = Offset(size.width * 0.65f, size.height * 0.08f),
            end = Offset(size.width * 0.5f, size.height * 0.27f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        // 电视外框
        drawRoundRect(
            color = TvPink,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke),
        )

        // 三个独立控制的点
        val dotY = top + (bottom - top) * 0.5f
        val dotRadius = size.width * 0.038f
        drawCircle(TvPink.copy(alpha = ui.dot1), dotRadius, Offset(size.width * 0.36f, dotY))
        drawCircle(TvPink.copy(alpha = ui.dot2), dotRadius, Offset(size.width * 0.50f, dotY))
        drawCircle(TvPink.copy(alpha = ui.dot3), dotRadius, Offset(size.width * 0.64f, dotY))
    }
}
