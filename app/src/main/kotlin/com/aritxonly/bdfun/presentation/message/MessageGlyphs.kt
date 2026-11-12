package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor

/* ---------------------------------------------------------------- glyphs */

/** 快捷入口三个图标统一按这个尺寸绘制，视觉大小保持一致。 */
internal val ShortcutGlyphSize = 28.dp

@Composable
internal fun MarkReadGlyph() {
    val color = teakGlassGlyphColor()
    Canvas(modifier = Modifier.size(21.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.12f
        // 双勾：表示“全部已读/清理未读”
        drawLine(
            color,
            Offset(w * 0.08f, h * 0.55f),
            Offset(w * 0.34f, h * 0.8f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(w * 0.34f, h * 0.8f),
            Offset(w * 0.78f, h * 0.24f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(w * 0.5f, h * 0.62f),
            Offset(w * 0.66f, h * 0.8f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(w * 0.66f, h * 0.8f),
            Offset(w * 0.94f, h * 0.48f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
internal fun ThreeDotsGlyph() {
    val color = teakGlassGlyphColor()
    Canvas(modifier = Modifier.size(21.dp)) {
        val r = size.width * 0.07f
        listOf(0.2f, 0.5f, 0.8f).forEach { y ->
            drawCircle(color, r, Offset(size.width * 0.5f, size.height * y))
        }
    }
}
