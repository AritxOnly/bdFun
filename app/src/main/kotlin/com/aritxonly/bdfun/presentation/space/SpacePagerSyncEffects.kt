package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun SpacePagerSyncEffects(
    content: SpaceScreenContent, scroll: SpaceScrollContext, actions: SpaceScrollActions,
) {
    val ui = content.ui
    val pagerState = content.pagerState
    val wideSpace = scroll.layout.wideSpace
    val statusBarTop = scroll.layout.statusBarTop
    // pager 和 ui.selectedTab 双向同步：左右滑或点 Tab 都会更新数据加载状态。
    LaunchedEffect(pagerState, wideSpace, statusBarTop) {
        // 等滑动真正停下来再改 ui.selectedTab，避免划过中间页时误触发那一页的加载。
        snapshotFlow { pagerState.settledPage }.collect { page ->
            actions.retainPinnedHeader(page)
            ui.selectedTab = page
        }
    }

}
