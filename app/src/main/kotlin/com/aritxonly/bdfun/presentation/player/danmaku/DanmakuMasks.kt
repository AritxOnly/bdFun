package com.aritxonly.bdfun.ui

import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import com.android.purebilibili.danmaku.engine.DanmakuMaskFrame


@Composable
internal fun rememberDanmakuMasks(portraitGuard: Boolean, viewSize: IntSize): List<DanmakuMaskFrame> {
    return remember(portraitGuard, viewSize) {
        if (!portraitGuard || viewSize.width <= 0 || viewSize.height <= 0) {
            emptyList()
        } else {
            val w = viewSize.width.toFloat()
            val h = viewSize.height.toFloat()
            val path = Path().apply {
                addRect(0f, h * 0.30f, w, h * 0.70f, Path.Direction.CW)
            }
            listOf(
                DanmakuMaskFrame(
                    startTimeMs = 0L,
                    endTimeMs = Long.MAX_VALUE,
                    path = path,
                    sourceWidth = viewSize.width,
                    sourceHeight = viewSize.height,
                ),
            )
        }
    }

}
