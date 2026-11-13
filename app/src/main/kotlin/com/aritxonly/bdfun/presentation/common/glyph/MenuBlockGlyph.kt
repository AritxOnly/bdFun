package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawAppMenuBlockGlyph(tint: Color, w: Float, h: Float, stroke: Stroke) {
    drawCircle(color = tint, radius = w * 0.34f, style = stroke)
    drawLine(
        color = tint,
        start = Offset(w * 0.28f, h * 0.72f),
        end = Offset(w * 0.72f, h * 0.28f),
        strokeWidth = w * 0.095f,
        cap = StrokeCap.Round,
    )

}
