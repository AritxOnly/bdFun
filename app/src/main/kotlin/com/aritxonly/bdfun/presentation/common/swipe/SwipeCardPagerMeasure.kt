package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal fun androidx.compose.ui.layout.SubcomposeMeasureScope.measureSwipeCardPager(runtime: SwipeCardPagerRuntime,
    gapPx: Float, constraints: Constraints, content: @Composable (Int) -> Unit): androidx.compose.ui.layout.MeasureResult = with(runtime) {
    val width = constraints.maxWidth.coerceAtLeast(1)
    val pages = (0 until pageCount).map { index ->
        subcompose(index) {
            Box(modifier = Modifier.fillMaxWidth()) { content(index) }
        }.map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
    }
    val heights = pages.map { items -> items.maxOfOrNull { it.height } ?: 0 }
    val lower = floor(progress).toInt().coerceIn(0, pageCount - 1)
    val upper = ceil(progress).toInt().coerceIn(0, pageCount - 1)
    // 滑动过程中取两页较高的高度，避免把正在进入的卡片裁剪掉；
    // 停稳后自然回到目标页自己的高度。
    val layoutHeight = maxOf(heights[lower], heights[upper])
    layout(width, layoutHeight) {
        val step = width + gapPx
        pages.forEachIndexed { index, placeables ->
            val x = ((index - progress) * step).roundToInt()
            placeables.forEach { placeable -> placeable.place(x, 0) }
        }
    }
}
