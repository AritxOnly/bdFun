package com.aritxonly.teak.common.chrome

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 仅限制可读宽度；需要宽屏留白可滚动时使用TeakScrollContainer。 */
fun Modifier.centeredListWidth(maxWidth: Dp? = null): Modifier = this
    .layout { measurable, constraints ->
        val contentWidth = resolveTeakContentWidth(
            if (constraints.hasBoundedWidth) constraints.maxWidth else null,
            maxWidth?.roundToPx(), 600.dp.roundToPx(), 680.dp.roundToPx(),
        )
        val child = measurable.measure(constraints.copy(
            minWidth = constraints.minWidth.coerceAtMost(contentWidth),
            maxWidth = contentWidth,
        ))
        val viewportWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else child.width
        layout(viewportWidth, child.height) {
            child.placeRelative((viewportWidth - child.width) / 2, 0)
        }
}
