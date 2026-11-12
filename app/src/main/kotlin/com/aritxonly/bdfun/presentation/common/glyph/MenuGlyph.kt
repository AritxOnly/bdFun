package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 评论长按菜单的小图标（1.6dp 描边、圆头）。 */
@Composable
fun MenuGlyph(kind: MenuGlyphKind, tint: Color, size: Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = w * 0.095f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (kind) {
            MenuGlyphKind.Reply -> drawAppMenuReplyGlyph(tint, w, h, stroke)
            MenuGlyphKind.Copy -> drawAppMenuCopyGlyph(tint, w, h, stroke)
            MenuGlyphKind.Report -> drawAppMenuReportGlyph(tint, w, h, stroke)
            MenuGlyphKind.Block -> drawAppMenuBlockGlyph(tint, w, h, stroke)
            MenuGlyphKind.Delete -> drawAppMenuDeleteGlyph(tint, w, h, stroke)
            MenuGlyphKind.Coin -> drawAppMenuCoinGlyph(tint, w, h, stroke)
            MenuGlyphKind.Star -> drawAppMenuStarGlyph(tint, w, h, stroke)
        }
    }
}
