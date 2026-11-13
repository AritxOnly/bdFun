package com.aritxonly.bdfun

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal class SpaceScrollActions(content: SpaceScreenContent, scroll: SpaceScrollContext, private val scope: kotlinx.coroutines.CoroutineScope) {
    private val ui = content.ui
    private val pagerState = content.pagerState
    private val wideSpace = scroll.layout.wideSpace
    private val statusBarTop = scroll.layout.statusBarTop
    private val tabListStates = scroll.layout.tabListStates
    private val listState = scroll.listState
    private val header = scroll.layout.header
    private val headerMaxCollapsePx = scroll.layout.headerMaxCollapsePx
    private val blurDensity = scroll.density
    fun applyPageDrag(dragY: Float) {
        val maxCollapse = headerMaxCollapsePx
        if (dragY < 0f) {
            val collapse = (-dragY).coerceAtMost((maxCollapse - header.collapsePx).coerceAtLeast(0f))
            if (collapse > 0f) header.collapsePx += collapse
            val remain = -dragY - collapse
            if (remain > 0f) listState.dispatchRawDelta(remain)
        } else {
            val atListTop = listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
            if (header.collapsePx > 0f && atListTop) {
                header.collapsePx = (header.collapsePx - dragY).coerceAtLeast(0f)
            } else {
                listState.dispatchRawDelta(-dragY)
            }
        }
    }

    suspend fun retainPinnedHeader(target: Int) {
        if (wideSpace || target == ui.selectedTab || target !in tabListStates.indices) return
        val pinY = with(blurDensity) { (statusBarTop + 56.dp).roundToPx() }
        val current = tabListStates[ui.selectedTab]
        val marker = current.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "space-tabs" }
        val wasPinned = current.firstVisibleItemIndex > 0 || (marker != null && marker.offset <= pinY)
        val next = tabListStates[target]
        // Preserve each tab's content position, but do not reopen the profile when changing pinned tabs.
        if (wasPinned && next.firstVisibleItemIndex == 0) {
            next.scrollToItem(1, -pinY)
        }
    }

    fun selectTab(index: Int) {
        if (index !in 0..3 || index == ui.selectedTab) return
        scope.launch {
            retainPinnedHeader(index)
            ui.selectedTab = index
            pagerState.animateScrollToPage(index)
        }
    }

}
