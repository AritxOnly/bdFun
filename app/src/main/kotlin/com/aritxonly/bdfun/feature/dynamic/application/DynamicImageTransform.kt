package com.aritxonly.bdfun.feature.dynamic.application

internal data class DynamicImageTransform(val scale: Float, val panX: Float, val panY: Float, val consume: Boolean)

/** 保留大图查看器的双指缩放与单指平移阈值，未放大时把单指滑动交还 pager。 */
internal fun dynamicImageTransform(
    scale: Float, panX: Float, panY: Float, pressedCount: Int,
    zoomChange: Float, changeX: Float, changeY: Float,
): DynamicImageTransform = when {
    pressedCount >= 2 -> {
        val next = (scale * zoomChange).coerceIn(1f, 4f)
        DynamicImageTransform(next, if (next > 1f) panX + changeX else 0f,
            if (next > 1f) panY + changeY else 0f, true)
    }
    scale > 1.01f -> DynamicImageTransform(scale, panX + changeX, panY + changeY, true)
    else -> DynamicImageTransform(scale, 0f, 0f, false)
}
