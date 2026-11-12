package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawAppMenuCopyGlyph(tint: Color, w: Float, h: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.28f, h * 0.12f),
        size = Size(w * 0.50f, h * 0.50f),
        cornerRadius = CornerRadius(w * 0.10f),
        style = stroke,
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.16f, h * 0.34f),
        size = Size(w * 0.50f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.10f),
        style = stroke,
    )

}
