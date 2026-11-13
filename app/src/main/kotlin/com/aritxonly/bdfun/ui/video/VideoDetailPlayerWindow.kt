package com.aritxonly.bdfun.ui


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoDetailPlayerWindow(
    content: VideoScreenContent, presentation: VideoScreenPresentation, events: VideoScreenEvents,
    inlineLayout: VideoInlineLayout, viewport: VideoPlayerViewport,
    pageUi: VideoDetailUiStateHolder, actions: VideoDetailViewActions,
    metadataController: com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val VideoPageBus = LocalVideoPageState.current
    val video = content.video
    val detail = content.read.detail
    val playInfo = content.read.playInfo
    val activeQuality = content.quality
    val danmaku = content.read.danmaku
    val selectionState = content.selection
    val interactionState = content.interaction
    val metadataState = content.metadata
    val fullscreen by pageUi.fullscreen
    val immersiveStatusBar = presentation.immersiveStatusBar
    val pictureInPicture = presentation.pictureInPicture
    VideoDetailPlayerViewport(presentation, inlineLayout, viewport, pageUi,
        publishBottom = { VideoPageBus.playerBottomPx = it }) {
        // 画面直接铺满不断收缩的播放器窗口：BiliPai 的 continuousPlayerViewportHeight。
        // 窗口变矮时 PlayerView 会按高度贴合，视频底边始终贴住窗口底边（Tab 行）。
        val playerAssembly = assembleVideoPlayer(
            VideoPlayerContent(video, detail, playInfo, activeQuality, danmaku, selectionState,
                interactionState, metadataState, content.loggedIn),
            VideoPlayerFlags(fullscreen, immersiveStatusBar, pictureInPicture),
        )
        VideoPlayer(
            media = playerAssembly.media,
            presentation = playerAssembly.presentation,
            actions = VideoDetailPlayerActionBindings(scope, content.read.detail, pageUi, actions, metadataController, events).create(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
