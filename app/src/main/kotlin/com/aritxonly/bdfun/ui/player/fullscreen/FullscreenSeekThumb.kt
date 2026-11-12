package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun TvSeekThumb(
    look: Float,
    modifier: Modifier = Modifier,
) {

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.094f
        val bodyLeft = w * 0.10f
        val bodyTop = h * 0.28f
        val bodyRight = w * 0.90f
        val bodyBottom = h * 0.88f
        val radius = w * 0.13f

        // 天线
        drawLine(
            color = Color.Black,
            start = Offset(w * 0.32f, h * 0.06f),
            end = Offset(w * 0.5f, h * 0.29f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.Black,
            start = Offset(w * 0.68f, h * 0.06f),
            end = Offset(w * 0.5f, h * 0.29f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        // 白色实体
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(radius, radius),
        )
        // 黑色轮廓
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke),
        )

        // 眼睛随滑动位置连续转动
        val eyeShift = look.coerceIn(-1f, 1f) * w * 0.045f
        val eyeWidth = w * 0.091f
        val eyeHeight = h * 0.21f
        val eyeTop = bodyTop + (bodyBottom - bodyTop) * 0.48f - eyeHeight / 2f
        val eyeRadius = CornerRadius(eyeWidth / 2f, eyeWidth / 2f)
        listOf(w * 0.35f + eyeShift, w * 0.65f + eyeShift).forEach { cx ->
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(cx - eyeWidth / 2f, eyeTop),
                size = Size(eyeWidth, eyeHeight),
                cornerRadius = eyeRadius,
            )
        }
    }
}
