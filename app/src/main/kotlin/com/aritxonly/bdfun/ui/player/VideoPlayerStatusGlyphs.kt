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
import androidx.compose.ui.unit.dp

internal fun describeMediaUrl(url: String?): String {
    if (url.isNullOrBlank()) return "none"
    return runCatching {
        val uri = android.net.Uri.parse(url)
        "${uri.host}${uri.path}"
    }.getOrDefault("invalid")
}

/** Fullscreen lock glyph: shackle closed when locked, open when unlocked. */
@Composable
internal fun LockGlyph(locked: Boolean) {

    Canvas(modifier = Modifier.size(23.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // lock body
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.22f, h * 0.45f),
            size = Size(w * 0.56f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        // shackle: full arc when locked, open arc when unlocked
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = if (locked) 180f else 140f,
            useCenter = false,
            topLeft = Offset(
                w * if (locked) 0.32f else 0.30f,
                h * if (locked) 0.18f else 0.12f,
            ),
            size = Size(w * 0.36f, h * 0.46f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
    }
}

internal fun sponsorCategoryLabel(category: String): String = when (category) {
    "sponsor" -> "赞助"
    "selfpromo" -> "自我推广"
    "interaction" -> "互动提醒"
    "intro" -> "开场"
    "outro" -> "结尾"
    "preview" -> "预览"
    "music_offtopic" -> "非音乐部分"
    "filler" -> "填充"
    else -> "片段"
}
