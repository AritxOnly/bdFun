package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import com.aritxonly.bdfun.core.application.PlaybackController
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun GestureIconTip(
    brightness: Boolean,
    value: Float,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val stroke = 1.7.dp.toPx()
            if (brightness) drawGestureBrightness(stroke) else drawGestureVolume(stroke)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(Capsule())
                    .background(Color(0xFFFB7299)),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGestureBrightness(stroke: Float) {
    drawCircle(
        color = Color.White,
        radius = size.width * 0.19f,
        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
    )
    repeat(8) { i ->
        val angle = Math.toRadians((i * 45).toDouble())
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r1 = size.width * 0.31f
        val r2 = size.width * 0.44f
        drawLine(
            color = Color.White,
            start = Offset(
                cx + (r1 * cos(angle)).toFloat(),
                cy + (r1 * sin(angle)).toFloat(),
            ),
            end = Offset(
                cx + (r2 * cos(angle)).toFloat(),
                cy + (r2 * sin(angle)).toFloat(),
            ),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGestureVolume(stroke: Float) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * 0.12f, size.height * 0.38f)
        lineTo(size.width * 0.32f, size.height * 0.38f)
        lineTo(size.width * 0.55f, size.height * 0.16f)
        lineTo(size.width * 0.55f, size.height * 0.84f)
        lineTo(size.width * 0.32f, size.height * 0.62f)
        lineTo(size.width * 0.12f, size.height * 0.62f)
        close()
    }
    drawPath(
        path = path,
        color = Color.White,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        ),
    )
    drawArc(
        color = Color.White,
        startAngle = -55f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(size.width * 0.52f, size.height * 0.3f),
        size = Size(size.width * 0.3f, size.height * 0.4f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
    )
}

@Composable
internal fun GestureTip(label: String, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

fun togglePlay(playback: PlaybackController, playing: Boolean) {
    if (playing) playback.pause() else playback.play()
}
