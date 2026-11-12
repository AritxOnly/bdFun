package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 播放器设置行的小图标，统一 22dp 线性风格。 */
private val GlyphSize = 22.dp

@Composable
fun AutoPlayNextGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.6.dp.toPx()
        val path = Path().apply {
            moveTo(s * 0.28f, s * 0.22f)
            lineTo(s * 0.28f, s * 0.78f)
            lineTo(s * 0.74f, s * 0.5f)
            close()
        }
        drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(
            tint,
            Offset(s * 0.84f, s * 0.24f),
            Offset(s * 0.84f, s * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun LoopGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.6.dp.toPx()
        drawArc(
            tint,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke, stroke),
            size = Size(s - stroke * 2, s - stroke * 2),
        )
        drawLine(
            tint,
            Offset(s * 0.66f, s * 0.06f),
            Offset(s * 0.88f, s * 0.26f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(s * 0.88f, s * 0.26f),
            Offset(s * 0.64f, s * 0.32f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun MirrorGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.6.dp.toPx()
        // 左右两个错位的方框 + 虚线，表示镜像
        drawRoundRect(
            tint,
            topLeft = Offset(s * 0.08f, s * 0.24f),
            size = Size(s * 0.34f, s * 0.52f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRoundRect(
            tint.copy(alpha = 0.5f),
            topLeft = Offset(s * 0.58f, s * 0.24f),
            size = Size(s * 0.34f, s * 0.52f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        var y = s * 0.30f
        while (y < s * 0.74f) {
            drawLine(
                tint,
                Offset(s * 0.48f, y),
                Offset(s * 0.52f, y),
                strokeWidth = 1.2.dp.toPx(),
            )
            y += s * 0.10f
        }
    }
}

@Composable
fun BackgroundListenGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.7.dp.toPx()
        drawArc(
            tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(s * 0.14f, s * 0.22f),
            size = Size(s * 0.72f, s * 0.54f),
        )
        drawLine(
            tint,
            Offset(s * 0.14f, s * 0.48f),
            Offset(s * 0.14f, s * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(s * 0.86f, s * 0.48f),
            Offset(s * 0.86f, s * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun ResizeGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.6.dp.toPx()
        // 左上 + 右下两个拐角
        val path = Path().apply {
            moveTo(s * 0.16f, s * 0.42f)
            lineTo(s * 0.16f, s * 0.16f)
            lineTo(s * 0.42f, s * 0.16f)
            moveTo(s * 0.58f, s * 0.84f)
            lineTo(s * 0.84f, s * 0.84f)
            lineTo(s * 0.84f, s * 0.58f)
        }
        drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(
            tint,
            Offset(s * 0.30f, s * 0.70f),
            Offset(s * 0.70f, s * 0.30f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun ColorAssistGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        drawCircle(tint, radius = s * 0.17f, center = Offset(s * 0.5f, s * 0.28f), style = Stroke(stroke))
        drawCircle(tint, radius = s * 0.17f, center = Offset(s * 0.28f, s * 0.68f), style = Stroke(stroke))
        drawCircle(tint, radius = s * 0.17f, center = Offset(s * 0.72f, s * 0.68f), style = Stroke(stroke))
    }
}

@Composable
fun SpeedGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.6.dp.toPx()
        drawArc(
            tint,
            startAngle = 160f,
            sweepAngle = 220f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(s * 0.12f, s * 0.18f),
            size = Size(s * 0.76f, s * 0.76f),
        )
        drawLine(
            tint,
            Offset(s * 0.5f, s * 0.56f),
            Offset(s * 0.7f, s * 0.34f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun TimerGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.6.dp.toPx()
        drawCircle(tint, radius = s * 0.34f, center = Offset(s * 0.5f, s * 0.54f), style = Stroke(stroke))
        drawLine(tint, Offset(s * 0.5f, s * 0.54f), Offset(s * 0.5f, s * 0.36f), strokeWidth = stroke)
        drawLine(tint, Offset(s * 0.5f, s * 0.54f), Offset(s * 0.66f, s * 0.62f), strokeWidth = stroke)
        drawLine(tint, Offset(s * 0.38f, s * 0.12f), Offset(s * 0.62f, s * 0.12f), strokeWidth = stroke)
    }
}

@Composable
fun SubtitleGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        drawRoundRect(
            tint,
            topLeft = Offset(s * 0.1f, s * 0.22f),
            size = Size(s * 0.8f, s * 0.56f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        drawLine(tint, Offset(s * 0.26f, s * 0.62f), Offset(s * 0.5f, s * 0.62f), strokeWidth = stroke)
        drawLine(tint, Offset(s * 0.56f, s * 0.62f), Offset(s * 0.74f, s * 0.62f), strokeWidth = stroke)
    }
}

@Composable
fun DanmakuSettingGlyph(tint: Color = Color.White) {
    Canvas(Modifier.size(GlyphSize)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        listOf(0.3f, 0.5f, 0.7f).forEachIndexed { index, y ->
            drawLine(
                tint,
                Offset(s * 0.12f, s * y),
                Offset(s * 0.88f, s * y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val knobX = when (index) {
                0 -> 0.62f
                1 -> 0.34f
                else -> 0.72f
            }
            drawCircle(tint, radius = s * 0.075f, center = Offset(s * knobX, s * y))
        }
    }
}

@Composable
fun DislikeGlyph(tint: Color = Color.White, sizeDp: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(Modifier.size(sizeDp)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        drawCircle(tint, radius = s * 0.36f, center = Offset(s / 2f, s / 2f), style = Stroke(stroke))
        // x 眼
        drawLine(tint, Offset(s * 0.34f, s * 0.40f), Offset(s * 0.44f, s * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.44f, s * 0.40f), Offset(s * 0.34f, s * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.56f, s * 0.40f), Offset(s * 0.66f, s * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.66f, s * 0.40f), Offset(s * 0.56f, s * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
        // 平嘴
        drawLine(tint, Offset(s * 0.38f, s * 0.64f), Offset(s * 0.62f, s * 0.64f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun WatchLaterGlyph(tint: Color = Color.White, sizeDp: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(Modifier.size(sizeDp)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        drawCircle(tint, radius = s * 0.34f, center = Offset(s * 0.46f, s * 0.54f), style = Stroke(stroke))
        drawLine(tint, Offset(s * 0.46f, s * 0.54f), Offset(s * 0.46f, s * 0.38f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.46f, s * 0.54f), Offset(s * 0.60f, s * 0.60f), strokeWidth = stroke, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(s * 0.72f, s * 0.30f)
            lineTo(s * 0.86f, s * 0.44f)
            lineTo(s * 0.72f, s * 0.58f)
        }
        drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
internal fun CacheGlyphSmall(tint: Color = Color.White, sizeDp: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(Modifier.size(sizeDp)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        drawLine(tint, Offset(s * 0.5f, s * 0.14f), Offset(s * 0.5f, s * 0.58f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.36f, s * 0.44f), Offset(s * 0.5f, s * 0.58f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.64f, s * 0.44f), Offset(s * 0.5f, s * 0.58f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.18f, s * 0.66f), Offset(s * 0.18f, s * 0.80f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.18f, s * 0.80f), Offset(s * 0.82f, s * 0.80f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(s * 0.82f, s * 0.80f), Offset(s * 0.82f, s * 0.66f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun PipGlyphSmall(tint: Color = Color.White, sizeDp: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(Modifier.size(sizeDp)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        drawRoundRect(
            tint,
            topLeft = Offset(s * 0.10f, s * 0.20f),
            size = Size(s * 0.80f, s * 0.60f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(s * 0.48f, s * 0.48f),
            size = Size(s * 0.36f, s * 0.26f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

@Composable
fun CastGlyphSmall(tint: Color = Color.White, sizeDp: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(Modifier.size(sizeDp)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        drawRoundRect(
            tint,
            topLeft = Offset(s * 0.12f, s * 0.20f),
            size = Size(s * 0.76f, s * 0.58f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        drawArc(
            tint,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(s * 0.12f, s * 0.48f),
            size = Size(s * 0.40f, s * 0.40f),
        )
        drawCircle(tint, radius = s * 0.06f, center = Offset(s * 0.20f, s * 0.72f))
    }
}
