package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
internal fun InlineBackGlyph() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.64f, size.height * 0.12f)
            lineTo(size.width * 0.28f, size.height * 0.5f)
            lineTo(size.width * 0.64f, size.height * 0.88f)
        }
        drawPath(
            path = path,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
internal fun InlineHomeGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(21.dp)) {
        val stroke = 2.2.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.48f)
            lineTo(size.width * 0.5f, size.height * 0.13f)
            lineTo(size.width * 0.88f, size.height * 0.48f)
            moveTo(size.width * 0.23f, size.height * 0.44f)
            lineTo(size.width * 0.23f, size.height * 0.86f)
            lineTo(size.width * 0.77f, size.height * 0.86f)
            lineTo(size.width * 0.77f, size.height * 0.44f)
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
internal fun InlineHeadphoneGlyph() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 2.1.dp.toPx()
        drawArc(
            color = Color.White,
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(size.width * 0.12f, size.height * 0.15f),
            size = Size(size.width * 0.76f, size.height * 0.62f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.1f, size.height * 0.5f),
            size = Size(size.width * 0.19f, size.height * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.71f, size.height * 0.5f),
            size = Size(size.width * 0.19f, size.height * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
    }
}

@Composable
internal fun InlineCastGlyph() {
    Canvas(modifier = Modifier.size(23.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
            size = Size(size.width * 0.84f, size.height * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.4f, size.height * 0.34f)
            lineTo(size.width * 0.4f, size.height * 0.56f)
            lineTo(size.width * 0.6f, size.height * 0.45f)
            close()
        }
        drawPath(path, Color.White)
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.36f, size.height * 0.9f),
            end = Offset(size.width * 0.64f, size.height * 0.9f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun InlineMoreGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(22.dp)) {
        repeat(3) { i ->
            drawCircle(
                color = tint,
                radius = 1.8.dp.toPx(),
                center = Offset(size.width / 2f, size.height * (0.24f + i * 0.26f)),
            )
        }
    }
}

@Composable
internal fun InlinePlayGlyph() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.26f, size.height * 0.1f)
            lineTo(size.width * 0.26f, size.height * 0.9f)
            lineTo(size.width * 0.88f, size.height * 0.5f)
            close()
        }
        drawPath(path, Color.White)
    }
}

@Composable
internal fun InlinePauseGlyph() {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.2f, size.height * 0.08f),
            size = Size(size.width * 0.18f, size.height * 0.84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.62f, size.height * 0.08f),
            size = Size(size.width * 0.18f, size.height * 0.84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
        )
    }
}

@Composable
internal fun InlineFullscreenGlyph() {
    Canvas(modifier = Modifier.size(21.dp)) {
        val stroke = 2.dp.toPx()
        val p = size.width * 0.12f
        val l = size.width * 0.4f
        // 四个角
        drawLine(Color.White, Offset(p, l), Offset(p, p), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(p, p), Offset(l, p), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(size.width - p, l), Offset(size.width - p, p), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(size.width - p, p), Offset(size.width - l, p), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(p, size.height - l), Offset(p, size.height - p), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(p, size.height - p), Offset(l, size.height - p), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(size.width - p, size.height - l), Offset(size.width - p, size.height - p), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(size.width - p, size.height - p), Offset(size.width - l, size.height - p), stroke, StrokeCap.Round)
    }
}

@Composable
internal fun DanmakuSettingsGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(19.dp)) {
        val w = size.width
        val stroke = w * 0.10f
        val ys = listOf(0.24f, 0.50f, 0.76f)
        val knobX = listOf(0.62f, 0.38f, 0.58f)
        ys.forEachIndexed { index, y ->
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(w * 0.10f, w * y),
                end = androidx.compose.ui.geometry.Offset(w * 0.90f, w * y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = tint,
                radius = w * 0.11f,
                center = androidx.compose.ui.geometry.Offset(w * knobX[index], w * y),
            )
        }
    }
}

/** 后台播放图标：耳机。 */
@Composable
internal fun BackgroundPlayGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(19.dp)) {
        val w = size.width
        val stroke = w * 0.10f
        drawArc(
            color = tint,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, w * 0.14f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, w * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = StrokeCap.Round,
            ),
        )
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, w * 0.50f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, w * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
        )
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.70f, w * 0.50f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, w * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
        )
    }
}
