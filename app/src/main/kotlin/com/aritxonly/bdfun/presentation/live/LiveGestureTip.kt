package com.aritxonly.bdfun.ui

import android.app.Activity
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun LiveGestureIconTip(
    brightness: Boolean,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveGestureSymbol(brightness)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFB7299)),
            )
        }
    }
}


internal fun liveCurrentBrightness(activity: Activity?): Float {
    val value = activity?.window?.attributes?.screenBrightness ?: -1f
    return if (value < 0f) 0.5f else value
}


internal fun liveApplyBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    window.attributes = window.attributes.apply {
        screenBrightness = value.coerceIn(0.02f, 1f)
    }
}


internal fun liveCurrentVolumeRatio(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}


internal fun liveSetVolumeRatio(audio: AudioManager, ratio: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    audio.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (ratio * max).toInt().coerceIn(0, max),
        0,
    )
}

/* ------------------------------------------------------------------ */
/* 弹幕设置：复用视频播放器的 DanmakuSettingsContent                      */
/* ------------------------------------------------------------------ */


@Composable
private fun LiveGestureSymbol(brightness: Boolean) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
            val stroke = 1.7.dp.toPx()
            if (brightness) {
                drawCircle(
                    color = Color.White,
                    radius = size.width * 0.19f,
                    style = Stroke(stroke),
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
                            cx + (r1 * kotlin.math.cos(angle)).toFloat(),
                            cy + (r1 * kotlin.math.sin(angle)).toFloat(),
                        ),
                        end = Offset(
                            cx + (r2 * kotlin.math.cos(angle)).toFloat(),
                            cy + (r2 * kotlin.math.sin(angle)).toFloat(),
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            } else {
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
                    style = Stroke(
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
                    size = androidx.compose.ui.geometry.Size(size.width * 0.3f, size.height * 0.4f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
}
