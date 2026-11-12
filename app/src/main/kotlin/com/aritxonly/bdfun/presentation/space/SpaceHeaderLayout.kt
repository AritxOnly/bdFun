package com.aritxonly.bdfun

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp

/** 把内容视觉上移，同时把布局底部多余的空白一起收掉，避免在 Tab 后面留色块。 */
internal fun Modifier.trimBottom(trim: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val trimPx = trim.roundToPx()
        val height = (placeable.height - trimPx).coerceAtLeast(0)
        layout(placeable.width, height) {
            placeable.place(0, -trimPx)
        }
    }

/** 头部容器：测量到高度后，按收起位移把占位一起缩短；内容本身由 graphicsLayer 平移。 */
internal fun Modifier.collapseHeaderContainer(
    measuredPx: Int,
    collapsePx: Float,
    trimPx: Int = 0,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = if (measuredPx > 0) {
            (measuredPx - trimPx - collapsePx).roundToInt().coerceAtLeast(0)
        } else {
            placeable.height
        }
        layout(placeable.width, height) {
            placeable.place(0, 0)
        }
    }
