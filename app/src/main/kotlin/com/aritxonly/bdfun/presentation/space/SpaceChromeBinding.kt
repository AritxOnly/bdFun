package com.aritxonly.bdfun

import androidx.compose.runtime.Composable

@Composable
internal fun SpaceChromeBinding(
    content: SpaceScreenContent, scroll: SpaceScrollContext, progress: SpaceScrollProgress,
    chrome: SpaceChromeState, actions: SpaceScrollActions,
) {
    val banner = content.banner
    val pagerState = content.pagerState
    val pagePosition = content.pagePosition
    val profile = content.snapshot.profile.space?.profile
    val loading = content.snapshot.profile.loading || !content.banner.ready
    val wideSpace = scroll.layout.wideSpace
    val profileWidth = scroll.layout.profileWidth
    val pageChrome = scroll.pageChrome
    val barBlurProgress = progress.blur.value
    val tabAnchor = progress.anchor.value
    // 下面这几个值都来自 snapshot state（滚动位置、pager 页码、加载状态）。
    // 必须在组合阶段读取，才会在滚动时触发重组；只在 SideEffect 里读的话，
    // 组合永远不会因滚动而重跑，写进顶栏的永远是首次进入时的值 ——
    // 表现就是：分段栏不出现、上滑也没有渐进模糊。
    val chromeHeaderProgress = barBlurProgress
    val chromeContentProgress = pageChrome.materialProgress
    val chromeTabY = tabAnchor
    val chromeVisible = !loading && banner.ready
    val chromeSelected = pagerState.currentPage

    androidx.compose.runtime.SideEffect {
        chrome.wide = wideSpace
        chrome.title = profile?.name.orEmpty().ifBlank { "个人主页" }
        chrome.profileWidth = profileWidth
        chrome.headerProgress = chromeHeaderProgress
        chrome.contentProgress = chromeContentProgress
        chrome.tabY = chromeTabY
        chrome.visible = chromeVisible
        chrome.selected = chromeSelected
        chrome.position = pagePosition
        chrome.onSelect = actions::selectTab
    }

}
