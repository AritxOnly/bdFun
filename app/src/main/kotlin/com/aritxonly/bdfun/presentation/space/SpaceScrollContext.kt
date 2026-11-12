package com.aritxonly.bdfun

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.RegisterScrollTop

internal data class SpaceScrollContext(
    val layout: SpaceScreenLayout, val listState: androidx.compose.foundation.lazy.LazyListState,
    val density: androidx.compose.ui.unit.Density, val bannerHeightPx: Float,
    val headerScrollState: androidx.compose.foundation.lazy.LazyListState,
    val pageChrome: com.aritxonly.teak.common.chrome.TeakPageScrollState,
)
internal data class SpaceScrollProgress(val blur: androidx.compose.runtime.State<Float>, val anchor: androidx.compose.runtime.State<Float>)
@Composable
internal fun rememberSpaceScrollContext(content: SpaceScreenContent): SpaceScrollContext {
    val pagerState = content.pagerState
    val pageChrome = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current
    val blurDensity = LocalDensity.current
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val windowWidth = with(blurDensity) { windowSize.width.toDp() }
    val wideSpace = windowWidth >= 736.dp
    // Compact lists include the profile and tab anchor; wide lists contain only posts.
    // Never let LazyColumn preserve a visible key from one structure into the other.
    val compactTabStates = List(4) { rememberLazyListState() }
    val wideTabStates = List(4) { rememberLazyListState() }
    val tabListStates = if (wideSpace) wideTabStates else compactTabStates
    val listState = tabListStates[pagerState.currentPage.coerceIn(0, 3)]
    RegisterScrollTop(listState, "space")

    val profileWidth = (windowWidth * 0.40f).coerceIn(280.dp, 440.dp)
    val profileListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val contentWidth = if (wideSpace) profileWidth else minOf(windowWidth, 680.dp)
    val bannerHeightPx = with(blurDensity) { (contentWidth * if (wideSpace) (1f / 2.35f) else 0.625f).toPx() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerScrollState = if (wideSpace) profileListState else listState

    // ---- 竖屏：资料头部整块收起（合并前的旧逻辑）；宽屏沿用现有滚动追踪 ----
    val header = remember { SpaceHeaderStateHolder() }
    val headerMaxCollapsePx = header.heightPx.toFloat()
    LaunchedEffect(headerMaxCollapsePx) {
        if (header.collapsePx > headerMaxCollapsePx) header.collapsePx = headerMaxCollapsePx
    }

    val headerScrollConnection = rememberSpaceHeaderConnection(header, headerMaxCollapsePx)
    return SpaceScrollContext(
        SpaceScreenLayout(wideSpace, profileWidth, statusBarTop, profileListState, tabListStates,
            headerScrollConnection, header, headerMaxCollapsePx),
        listState, blurDensity, bannerHeightPx, headerScrollState, pageChrome,
    )
}
