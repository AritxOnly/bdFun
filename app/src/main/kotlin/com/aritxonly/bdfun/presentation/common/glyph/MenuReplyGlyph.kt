package com.aritxonly.bdfun.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawAppMenuReplyGlyph(tint: Color, w: Float, h: Float, stroke: Stroke) {
    val path = Path().apply {
        moveTo(w * 0.16f, h * 0.46f)
        lineTo(w * 0.40f, h * 0.24f)
        moveTo(w * 0.16f, h * 0.46f)
        lineTo(w * 0.40f, h * 0.68f)
        moveTo(w * 0.18f, h * 0.46f)
        cubicTo(w * 0.52f, h * 0.46f, w * 0.66f, h * 0.50f, w * 0.82f, h * 0.72f)
    }
    drawPath(path, tint, style = stroke)

}
