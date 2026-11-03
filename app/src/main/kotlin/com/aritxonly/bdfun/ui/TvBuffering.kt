package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TvPink = Color(0xFFF48FB1)

/**
 * 播放器缓冲：矢量小电视 + 三个独立渐隐的省略号点 + 「正在缓冲...」。
 * 点动画顺序：左点渐显 → 中点渐显 → 右点渐显 → 左点渐隐 → 中点渐隐 → 右点渐隐 → 循环。
 */
@Composable
fun TvBuffering(
    modifier: Modifier = Modifier,
    tvSize: Dp = 66.dp,
    showedText: Boolean = true,
) {
    var dot1 by remember { mutableFloatStateOf(0f) }
    var dot2 by remember { mutableFloatStateOf(0f) }
    var dot3 by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (startNanos == 0L) startNanos = now
                val t = ((now - startNanos) / 1_000_000f) % 2700f

                dot1 = when {
                    t < 250f -> t / 250f
                    t < 1400f -> 1f
                    t < 1650f -> 1f - (t - 1400f) / 250f
                    else -> 0f
                }
                dot2 = when {
                    t < 250f -> 0f
                    t < 500f -> (t - 250f) / 250f
                    t < 1650f -> 1f
                    t < 1900f -> 1f - (t - 1650f) / 250f
                    else -> 0f
                }
                dot3 = when {
                    t < 500f -> 0f
                    t < 750f -> (t - 500f) / 250f
                    t < 1900f -> 1f
                    t < 2150f -> 1f - (t - 1900f) / 250f
                    else -> 0f
                }
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(tvSize)) {
            val stroke = size.width * 0.07f
            val left = size.width * 0.12f
            val top = size.height * 0.27f
            val right = size.width * 0.88f
            val bottom = size.height * 0.82f
            val radius = size.width * 0.13f

            // 两根圆头天线
            drawLine(
                color = TvPink,
                start = Offset(size.width * 0.35f, size.height * 0.08f),
                end = Offset(size.width * 0.5f, size.height * 0.27f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = TvPink,
                start = Offset(size.width * 0.65f, size.height * 0.08f),
                end = Offset(size.width * 0.5f, size.height * 0.27f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )

            // 电视外框
            drawRoundRect(
                color = TvPink,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = stroke),
            )

            // 三个独立控制的点
            val dotY = top + (bottom - top) * 0.5f
            val dotRadius = size.width * 0.038f
            drawCircle(TvPink.copy(alpha = dot1), dotRadius, Offset(size.width * 0.36f, dotY))
            drawCircle(TvPink.copy(alpha = dot2), dotRadius, Offset(size.width * 0.50f, dotY))
            drawCircle(TvPink.copy(alpha = dot3), dotRadius, Offset(size.width * 0.64f, dotY))
        }
        if (showedText) {
            Spacer(Modifier.height(0.dp))
            Text(
                text = "正在缓冲...",
                color = Color.White,
                fontSize = 11.sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 3f,
                    ),
                ),
                modifier = Modifier.offset(y = 0.dp),
            )
        }
    }
}
