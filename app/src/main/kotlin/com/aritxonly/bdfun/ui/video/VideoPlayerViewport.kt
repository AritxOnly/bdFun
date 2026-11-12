package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class VideoViewportInput(
    val expandedHeight: Dp, val collapsedHeight: Dp, val animatedHeight: Float,
    val statusTop: Dp, val immersiveStatusBar: Boolean,
    val fullscreen: Boolean, val pictureInPicture: Boolean, val splitPanes: Boolean,
)

internal data class VideoPlayerViewport(val collapseProgress: Float, val playerTopInset: Dp)

/** 画面顶部与收起条沿用同一个进度，避免窗口和 tab 分别计算造成错位。 */
internal fun resolveVideoPlayerViewport(input: VideoViewportInput): VideoPlayerViewport {
    val range = (input.expandedHeight - input.collapsedHeight).coerceAtLeast(0.dp)
    val progress = if (input.fullscreen || input.pictureInPicture || input.splitPanes || range.value <= 0f) {
        0f
    } else {
        ((input.expandedHeight.value - input.animatedHeight) / range.value).coerceIn(0f, 1f)
    }
    val top = if (input.immersiveStatusBar) input.statusTop * progress else input.statusTop
    return VideoPlayerViewport(progress, top)
}
