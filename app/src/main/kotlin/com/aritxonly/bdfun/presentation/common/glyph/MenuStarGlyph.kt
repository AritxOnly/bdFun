package com.aritxonly.bdfun.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawAppMenuStarGlyph(tint: Color, w: Float, h: Float, stroke: Stroke) {
    val path = Path().apply {
        val cx = w * 0.5f
        val cy = h * 0.5f
        val outer = w * 0.36f
        val inner = outer * 0.52f
        for (k in 0 until 10) {
            val r = if (k % 2 == 0) outer else inner
            val angle = (-90 + k * 36) * Math.PI / 180
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, tint, style = stroke)

}
