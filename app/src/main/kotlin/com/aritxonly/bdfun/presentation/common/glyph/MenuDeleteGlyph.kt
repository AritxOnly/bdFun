package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawAppMenuDeleteGlyph(tint: Color, w: Float, h: Float, stroke: Stroke) {
    drawLine(
        color = tint,
        start = Offset(w * 0.18f, h * 0.26f),
        end = Offset(w * 0.82f, h * 0.26f),
        strokeWidth = w * 0.095f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.40f, h * 0.26f),
        end = Offset(w * 0.40f, h * 0.16f),
        strokeWidth = w * 0.095f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.40f, h * 0.16f),
        end = Offset(w * 0.60f, h * 0.16f),
        strokeWidth = w * 0.095f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.60f, h * 0.16f),
        end = Offset(w * 0.60f, h * 0.26f),
        strokeWidth = w * 0.095f,
        cap = StrokeCap.Round,
    )
    val body = Path().apply {
        moveTo(w * 0.26f, h * 0.30f)
        lineTo(w * 0.30f, h * 0.84f)
        lineTo(w * 0.70f, h * 0.84f)
        lineTo(w * 0.74f, h * 0.30f)
    }
    drawPath(body, tint, style = stroke)
    listOf(0.42f, 0.58f).forEach { x ->
        drawLine(
            color = tint,
            start = Offset(w * x, h * 0.44f),
            end = Offset(w * x, h * 0.72f),
            strokeWidth = w * 0.075f,
            cap = StrokeCap.Round,
        )
    }

}
