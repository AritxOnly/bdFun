package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.InlineVideoMedia
import kotlinx.coroutines.flow.StateFlow

/** 预览与详情播放器使用不同的原生参数；此契约只表达预览实际需要的命令。 */
interface InlinePlaybackController {
    val aspectRatio: StateFlow<Float>
    fun setMuted(muted: Boolean)
    fun prepare(media: InlineVideoMedia)
    fun play()
    fun pause()
    fun release()
}

fun inlinePlaybackAspect(width: Int, height: Int, pixelWidthHeightRatio: Float, current: Float): Float {
    if (width <= 0 || height <= 0) return current
    val ratio = width * pixelWidthHeightRatio / height.toFloat()
    return if (ratio.isFinite() && ratio > 0.05f) ratio else current
}
