package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig

private const val LIVE_DM_BASE_TRAVEL_MS = 8_500f
internal class LiveDanmakuUiStateHolder(initialId: Long) {
    var viewSize by mutableStateOf(IntSize.Zero)
    var lastSeenId by mutableLongStateOf(initialId)
}
internal class LiveDanmakuClockStateHolder {
    var pausedAt by mutableLongStateOf(0L)
    var pausedTotal by mutableLongStateOf(0L)
}
@Composable
internal fun liveDanmakuConfig(viewSize: IntSize, fullscreen: Boolean): DanmakuRenderConfig {
    val settings = LocalSettingsBindings.current
    val density = LocalDensity.current
    val area = settings.danmakuArea.coerceIn(0.15f, 1f)
    val speed = settings.danmakuSpeedPref.coerceAtLeast(0.1f)
    val fontScale = if (fullscreen) {
        settings.danmakuScaleFullscreen
    } else {
        settings.danmakuScaleInline
    }
    val textSizePx = with(density) { 12.sp.toPx() } * fontScale
    val strokePx = with(density) { settings.danmakuStroke.dp.toPx() }
    val lineHeightPx = (textSizePx * 1.9f).coerceAtLeast(1f)
    val lineCount = if (viewSize.height > 0) {
        (((viewSize.height * area) / lineHeightPx).toInt()).coerceIn(1, 9)
    } else {
        1
    }
    val bottomMarginPx = with(density) {
        (if (fullscreen) 44.dp else 42.dp).toPx()
    }
    return DanmakuRenderConfig(
        alpha = 255,
        textSizePx = textSizePx,
        typeface = remember { Typeface.create("sans-serif-medium", Typeface.NORMAL) },
        strokeWidthPx = strokePx,
        strokeColor = 0xE6000000.toInt(),
        scrollDurationMs = (LIVE_DM_BASE_TRAVEL_MS / speed)
            .toLong()
            .coerceAtLeast(1L),
        lineHeightPx = lineHeightPx,
        lineMarginPx = 0f,
        lineCount = lineCount,
        topMarginPx = 0f,
        bottomMarginPx = bottomMarginPx,
        pinnedDurationMs = 4_000L,
    )

}

@Composable
internal fun LiveDanmakuClock(engine: com.android.purebilibili.danmaku.engine.DanmakuEngine, playing: Boolean) {
    val startTime = remember { SystemClock.elapsedRealtime() }
    val ui = remember { LiveDanmakuClockStateHolder() }
    var pausedAt by ui::pausedAt
    var pausedTotal by ui::pausedTotal
    LaunchedEffect(playing) {
        if (playing) {
            if (pausedAt > 0L) {
                pausedTotal += SystemClock.elapsedRealtime() - pausedAt
                pausedAt = 0L
            }
            engine.start(
                (SystemClock.elapsedRealtime() - startTime - pausedTotal).coerceAtLeast(0L),
            )
        } else {
            pausedAt = SystemClock.elapsedRealtime()
            engine.pause()
        }
    }

}
