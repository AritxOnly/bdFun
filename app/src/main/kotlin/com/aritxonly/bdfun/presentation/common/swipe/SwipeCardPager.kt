package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SwipeCardPager(
    selected: Int,
    pageCount: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** 两张卡片之间留出的距离，不贴在一起。 */
    gap: Dp = 12.dp,
    /** 外部可以传同一个状态给 Tab 栏，让指示器跟着手指一起走。 */
    progressState: MutableFloatState? = null,
    content: @Composable (Int) -> Unit,
) {
    if (pageCount <= 0) return
    val scope = rememberCoroutineScope()
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    val sharedProgress = progressState ?: remember { SwipeCardPagerFallbackStateHolder(selected) }.progress
    val ui = remember { SwipeCardPagerPresentationStateHolder(selected) }
    val runtime = SwipeCardPagerRuntime(selected, pageCount, onSelected, sharedProgress, ui, scope)
    BindSwipeCardPagerSelection(runtime)
    SubcomposeLayout(
        modifier = modifier.fillMaxWidth().clipToBounds()
            .onSizeChanged { ui.widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .swipeCardPagerGestures(runtime),
    ) { constraints -> measureSwipeCardPager(runtime, gapPx, constraints, content) }
}
