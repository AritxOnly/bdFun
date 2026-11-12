package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawAppMenuCoinGlyph(tint: Color, w: Float, h: Float, stroke: Stroke) {
    drawCircle(color = tint, radius = w * 0.34f, style = stroke)
    drawLine(
        color = tint,
        start = Offset(w * 0.38f, h * 0.42f),
        end = Offset(w * 0.62f, h * 0.42f),
        strokeWidth = w * 0.085f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(w * 0.50f, h * 0.42f),
        end = Offset(w * 0.50f, h * 0.68f),
        strokeWidth = w * 0.085f,
        cap = StrokeCap.Round,
    )

}
