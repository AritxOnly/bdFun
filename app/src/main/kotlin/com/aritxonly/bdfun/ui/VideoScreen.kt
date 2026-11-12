package com.aritxonly.bdfun.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope

@Composable
internal fun VideoScreen(
    isLoggedIn: () -> Boolean, request: VideoScreenRequest, presentation: VideoScreenPresentation, events: VideoScreenEvents,
    pager: PagerState = rememberPagerState(pageCount = { 2 }),
) {
    val page = LocalVideoPageState.current
    val holders = rememberVideoScreenStateHolders(request)
    val detail by holders.controllers.detail.state.collectAsState()
    val selection by holders.controllers.selection.state.collectAsState()
    val interaction by holders.controllers.interaction.state.collectAsState()
    val metadata by holders.metadata.state.collectAsState()
    // 只更新当前标识；真正退出时仍由 Activity 清理读取缓存。
    LaunchedEffect(selection.bvid) { page.currentBvid = selection.bvid }
    val playing by holders.ui.playing
    val inlineLayout = rememberVideoInlineLayout(selection.bvid, playing, pager.currentPage,
        presentation.paneLayout, presentation.immersiveStatusBar, presentation.statusBarTop)
    val metaReady by holders.ui.metaReady
    // 保留页面作用域的唯一推荐入场进度，进出全屏不重建。
    val relatedEnter = rememberStaggerProgress(metaReady && detail.related.isNotEmpty(), 9)
    val scope = rememberCoroutineScope()
    val actions = rememberVideoDetailActions(isLoggedIn, request, holders, scope, events.onBack)
    val qualities = detail.playInfo?.qualities.orEmpty()
    val quality = if (selection.autoQuality) {
        page.automaticQuality.select(qualities)
    } else {
        qualities.firstOrNull { it.id == selection.quality } ?: qualities.firstOrNull()
    }
    val content = VideoScreenContent(request.video, detail, selection, interaction, metadata, quality, isLoggedIn())
    BindVideoScreenEffects( request, content, holders, actions, pager)
    VideoScreenLayout( request, presentation, events, pager, inlineLayout, holders, content, actions, relatedEnter, scope)
    VideoDetailSheets(holders.ui, interaction, actions)
}
