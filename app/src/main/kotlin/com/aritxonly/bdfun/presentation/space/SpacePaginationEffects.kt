package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun SpacePaginationEffects(
    runtime: SpaceScreenRuntime, scroll: SpaceScrollContext, actions: SpaceContentActions,
) {
    val content = runtime.content
    val ui = content.ui
    val listState = scroll.listState
    // 滚到底部时翻页。
    LaunchedEffect(runtime.controller, listState, ui.selectedTab, ui.submissionMode) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, total) ->
            if (last < 0 || total <= 0 || last < total - 3) return@collect
            actions.loadMore(ui.selectedTab, ui.submissionMode)
        }
    }

}
