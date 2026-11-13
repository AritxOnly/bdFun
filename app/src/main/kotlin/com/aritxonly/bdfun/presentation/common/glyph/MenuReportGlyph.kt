package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawAppMenuReportGlyph(tint: Color, w: Float, h: Float, stroke: Stroke) {
    drawLine(
        color = tint,
        start = Offset(w * 0.26f, h * 0.12f),
        end = Offset(w * 0.26f, h * 0.88f),
        strokeWidth = w * 0.095f,
        cap = StrokeCap.Round,
    )
    val flag = Path().apply {
        moveTo(w * 0.30f, h * 0.16f)
        lineTo(w * 0.78f, h * 0.16f)
        lineTo(w * 0.64f, h * 0.36f)
        lineTo(w * 0.78f, h * 0.56f)
        lineTo(w * 0.30f, h * 0.56f)
        close()
    }
    drawPath(flag, tint, style = stroke)

}
