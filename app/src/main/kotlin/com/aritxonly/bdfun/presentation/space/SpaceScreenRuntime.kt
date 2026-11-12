package com.aritxonly.bdfun

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

internal data class SpaceScreenRuntime(
    val content: SpaceScreenContent,
    val controller: com.aritxonly.bdfun.feature.space.application.SpaceStateHolder,
    val scope: kotlinx.coroutines.CoroutineScope,
)

@Composable
internal fun rememberSpaceScreenRuntime(
    mid: Long, session: com.aritxonly.bdfun.core.model.user.UserProfile?,
    controller: com.aritxonly.bdfun.feature.space.application.SpaceStateHolder,
): SpaceScreenRuntime {
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(Unit) {
        if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
            com.aritxonly.bdfun.data.LiveCounters.spaceInstances++
        }
        onDispose {
            if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
                com.aritxonly.bdfun.data.LiveCounters.spaceInstances--
            }
        }
    }
    val isSelf = session?.mid == mid
    val loggedIn = session != null
    val snapshot by controller.state.collectAsState()
    // 头图解码和亮度属于视图状态，不进入纯应用层。
    val banner = remember(mid) { SpaceBannerStateHolder() }

    val ui = remember { SpaceSelectionStateHolder() }
    // 每个内容分类保留独立列表；宽屏资料单独放在左栏。
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 4 })
    val pagePosition = remember(pagerState) {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    // 0 = 视频，1 = 图文，2 = 某个合集/系列

    return SpaceScreenRuntime(
        SpaceScreenContent(mid, isSelf, loggedIn, snapshot, ui, banner, pagerState, pagePosition), controller, scope,
    )
}
