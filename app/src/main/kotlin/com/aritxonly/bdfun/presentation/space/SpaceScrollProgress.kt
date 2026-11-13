package com.aritxonly.bdfun

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberSpaceBarBlur(
    scroll: SpaceScrollContext,
): androidx.compose.runtime.State<Float> {
    val wideSpace = scroll.layout.wideSpace
    val header = scroll.layout.header
    val bannerHeightPx = scroll.bannerHeightPx
    val headerScrollState = scroll.headerScrollState
    return remember(wideSpace, headerScrollState, bannerHeightPx) {
        derivedStateOf {
            if (!wideSpace) {
                (header.collapsePx / (bannerHeightPx * 0.5f)).coerceIn(0f, 1f)
            } else if (headerScrollState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (headerScrollState.firstVisibleItemScrollOffset / (bannerHeightPx * 0.5f)).coerceIn(0f, 1f)
            }
        }
    }

}

@Composable
internal fun rememberSpaceTabAnchor(
    scroll: SpaceScrollContext,
): androidx.compose.runtime.State<Float> {
    val wideSpace = scroll.layout.wideSpace
    val statusBarTop = scroll.layout.statusBarTop
    val listState = scroll.listState
    val blurDensity = scroll.density
    return remember(listState, wideSpace, statusBarTop, blurDensity) {
        derivedStateOf {
            val pinY = with(blurDensity) { (statusBarTop + 56.dp).toPx() }
            val info = listState.layoutInfo
            val tabs = info.visibleItemsInfo.firstOrNull { it.key == "space-tabs" }
            if (wideSpace) 0f else com.aritxonly.bdfun.ui.spaceTabOffset(
                listState.firstVisibleItemIndex, tabs?.offset, pinY,
            )
        }
    }

}
