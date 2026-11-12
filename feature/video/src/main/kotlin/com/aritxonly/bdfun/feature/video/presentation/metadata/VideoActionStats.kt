package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun ActionStat(
    modifier: Modifier = Modifier,
    label: String,
    value: Long,
    kind: ActionKind,
    iconRes: Int, accentColor: Color, activity: android.app.Activity?,
    active: Boolean = false,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val state = remember { VideoActionPressStateHolder() }
    VideoActionPressDecay(state, kind)
    val colorTarget = if (active || state.isPressing || state.press > 0.01f) accentColor else colors.onBackgroundVariant
    val animatedTint by androidx.compose.animation.animateColorAsState(
        targetValue = colorTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 240,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "action-tint",
    )
    VideoActionLongPress(state, onLongPress)
    VideoActionVisual(modifier, label, value, iconRes, onClick, onLongPress != null, state, animatedTint, activity)
}

internal enum class ActionKind { ThumbUp, ThumbDown, Coin, Star, Share }

@Composable
internal fun MiniStat(text: String, playIcon: Boolean) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
            if (playIcon) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.12f)
                    lineTo(size.width * 0.3f, size.height * 0.88f)
                    lineTo(size.width * 0.9f, size.height * 0.5f)
                    close()
                }
                drawPath(path, colors.onBackgroundVariant)
            } else {
                drawRoundRect(
                    color = colors.onBackgroundVariant,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.15f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.1f),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = colors.onBackgroundVariant, fontSize = 12.sp)
    }
}

@Composable
internal fun PersonGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 12.dp) {
    // 和播放器左上角「正在看」使用同一套双人观看图标。
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val stroke = 1.dp.toPx()
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            color = color,
            radius = w * 0.15f,
            center = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.32f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawCircle(
            color = color,
            radius = w * 0.13f,
            center = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.3f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.5f),
            size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.38f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.48f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.36f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
    }
}

/**
 * 可折叠简介：用 TextMeasurer 量出完整高度，再按手动进度裁剪高度，
 * 不依赖 frame clock 的 animateContentSize（本项目里它不推进）。
 */
