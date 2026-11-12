package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.abs


internal const val SYNC_INTERVAL_MS = 200L
internal const val SEEK_THRESHOLD_MS = 700L
@Composable
internal fun BindDanmakuEngineClock(engine: com.android.purebilibili.danmaku.engine.DanmakuEngine,
    playing: Boolean, positionMs: () -> Long) {
    // 播放器进度是唯一的时钟来源：每 200ms 对齐一次，拖动进度条时做大跳转。
    // 这样长按倍速、蓝牙延迟、暂停恢复都不用再单独处理。
    val playingNow by rememberUpdatedState(playing)
    LaunchedEffect(engine) {
        var last = positionMs()
        while (true) {
            delay(SYNC_INTERVAL_MS)
            val current = positionMs()
            val jump = abs(current - last)
            if (playingNow) {
                if (jump > SEEK_THRESHOLD_MS) {
                    engine.seekTo(current.coerceAtLeast(0L))
                } else {
                    engine.synchronizeTo(current.coerceAtLeast(0L))
                }
            } else if (jump > SEEK_THRESHOLD_MS) {
                engine.seekTo(current.coerceAtLeast(0L))
            }
            last = current
        }
    }
}
