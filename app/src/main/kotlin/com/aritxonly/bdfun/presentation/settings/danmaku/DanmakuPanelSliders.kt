package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.aritxonly.bdfun.feature.settings.application.*

@Composable
internal fun DanmakuPanelSliders(runtime: DanmakuPanelRuntime) {
    val settings = runtime.settings
    val onInteract = runtime.onInteract
    val fullscreen = runtime.fullscreen
    val fontScale = if (fullscreen) settings.danmakuScaleFullscreen else settings.danmakuScaleInline
    DmGroup(title = "弹幕设置") {
        DmSliderRow(
            label = "显示区域",
            valueText = "${(settings.danmakuArea * 100).roundToInt()}%",
            value = settings.danmakuArea,
            range = 0.15f..1f,
            onChangeStart = onInteract,
        ) { settings.danmakuArea = it }

        DmSliderRow(
            label = "不透明度",
            valueText = "${(settings.danmakuOpacity * 100).roundToInt()}%",
            value = settings.danmakuOpacity,
            range = 0.2f..1f,
            onChangeStart = onInteract,
        ) { settings.danmakuOpacity = it }

        DmSliderRow(
            label = "字体大小",
            valueText = "${(fontScale * 100).roundToInt()}%",
            value = fontScale,
            range = 0.6f..2f,
            onChangeStart = onInteract,
        ) {
            if (fullscreen) {
                settings.danmakuScaleFullscreen = it
            } else {
                settings.danmakuScaleInline = it
            }
        }

        DmSliderRow(
            label = "弹幕速度",
            valueText = speedWord(settings.danmakuSpeedPref),
            value = settings.danmakuSpeedPref,
            range = 0.5f..2f,
            onChangeStart = onInteract,
        ) { settings.danmakuSpeedPref = it }

        DmSliderRow(
            label = "描边大小",
            valueText = String.format(java.util.Locale.US, "%.1f", settings.danmakuStroke),
            value = settings.danmakuStroke,
            range = 0f..4f,
            onChangeStart = onInteract,
        ) { settings.danmakuStroke = it }

        Spacer(Modifier.height(4.dp))
    }
}
