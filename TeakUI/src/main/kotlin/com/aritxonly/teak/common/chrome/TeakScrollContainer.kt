package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 居中的页面滚动视口。内容保持原来的 LazyColumn/Grid/verticalScroll 和滚动状态。
 *
 * 宽屏左右留白是内容的兄弟节点，只转发留白里的拖动/惯性到同一状态；
 * 不在内容祖先再次安装 scrollable，避免同一次拖动或 fling 经两层处理。
 */
@Composable
fun TeakScrollContainer(
    scrollState: ScrollableState?,
    modifier: Modifier = Modifier,
    maxWidth: Dp? = null,
    content: @Composable () -> Unit,
) {
    // 反向聊天列表与普通列表的状态增量方向相反；只在方向变化时重组。
    val reverseDirection by remember(scrollState) {
        derivedStateOf {
            when (scrollState) {
                is LazyListState -> !scrollState.layoutInfo.reverseLayout
                is LazyGridState -> !scrollState.layoutInfo.reverseLayout
                else -> true
            }
        }
    }
    val gutterModifier = if (scrollState == null) Modifier else Modifier.scrollable(
        state = scrollState,
        orientation = Orientation.Vertical,
        reverseDirection = reverseDirection,
    )
    Layout(modifier = modifier, content = {
        // 内容最后绘制，让内容内的弹出层仍拥有原来的命中优先级。
        Box(gutterModifier)
        Box(gutterModifier)
        Box { content() }
    }) { measurables, constraints ->
        val contentWidth = resolveTeakContentWidth(
            if (constraints.hasBoundedWidth) constraints.maxWidth else null,
            maxWidth?.roundToPx(), 600.dp.roundToPx(), 680.dp.roundToPx(),
        )
        val child = measurables[2].measure(constraints.copy(
            minWidth = constraints.minWidth.coerceAtMost(contentWidth),
            maxWidth = contentWidth,
        ))
        val viewportWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else child.width
        val startWidth = (viewportWidth - child.width) / 2
        val endWidth = viewportWidth - child.width - startWidth
        val start = measurables[0].measure(Constraints.fixed(startWidth, child.height))
        val end = measurables[1].measure(Constraints.fixed(endWidth, child.height))
        layout(viewportWidth, child.height) {
            start.placeRelative(0, 0)
            end.placeRelative(startWidth + child.width, 0)
            child.placeRelative(startWidth, 0)
        }
    }
}
