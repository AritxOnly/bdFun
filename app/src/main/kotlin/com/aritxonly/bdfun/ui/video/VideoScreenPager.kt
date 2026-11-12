package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.core.common.messageOr
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoScreenPager(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    pager: androidx.compose.foundation.pager.PagerState, inlineLayout: VideoInlineLayout,
    pageUi: VideoDetailUiStateHolder, description: VideoDescriptionStateHolder,
    content: VideoScreenContent, actions: VideoDetailViewActions, relatedEnter: Float,
) {
    val context = LocalContext.current
    val VideoPageBus = LocalVideoPageState.current
    val colors = MiuixTheme.colorScheme
    val fullscreen by pageUi.fullscreen
    val metaReady by pageUi.metaReady
    val pictureInPicture = presentation.pictureInPicture
    val paneLayout = presentation.paneLayout
    val splitPanes = paneLayout.isSplit && !fullscreen && !pictureInPicture
    val statusBarTop = presentation.statusBarTop
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val collapseConnection = inlineLayout.collapseConnection
    VideoDetailPager(
        VideoDetailPaneState(fullscreen, pictureInPicture, splitPanes, paneLayout, playerBlockHeight, statusBarTop),
        pager, collapseConnection,
        metadata = { VideoScreenMetadata(content, pageUi, description, actions) },
        tail = {
            videoScreenTail(content, metaReady, relatedEnter, colors.onBackgroundVariant, colors.surface, actions, context, VideoPageBus)
        },
        comments = { paneWidth ->
            VideoScreenComments( request, presentation, pager, inlineLayout, pageUi, content, paneWidth)
        },
    )
}

@Composable
private fun VideoScreenMetadata(
    content: VideoScreenContent, ui: VideoDetailUiStateHolder,
    description: VideoDescriptionStateHolder, actions: VideoDetailViewActions,
) {
    val VideoPageBus = LocalVideoPageState.current
    val videoContext = LocalContext.current
    val detail = content.read.detail
    val video = content.video
    val metaReady by ui.metaReady
    val upFollowers = content.metadata.followers
    val upVideos = content.metadata.videos
    val onlineCount = content.metadata.onlineCount
    val onlineText = content.metadata.onlineText
    val interactionState = content.interaction
    val metadata = rememberVideoMetadataAssembly()
    VideoMetadataSection(
        VideoMetadataContent(detail, video, metaReady, upFollowers, upVideos, onlineCount, onlineText),
        interactionState, description,
        VideoMetadataActions(actions::toggleFollow, actions::toggleLike, actions::toggleDislike, actions::giveCoin,
            actions::toggleFavorite, actions::shareVideo, actions::tripleAction,
            onOpenSpace = {
                VideoPageBus.pauseAll()
                navigateToSpace(videoContext, detail?.ownerMid ?: video.ownerMid)
            }),
        appearance = metadata.appearance, slots = metadata.slots,
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.videoScreenTail(
    content: VideoScreenContent, metaReady: Boolean, relatedEnter: Float,
    textColor: Color, surfaceColor: Color, actions: VideoDetailViewActions,
    context: android.content.Context, VideoPageBus: VideoPagePresentationState,
) {
    val detail = content.read.detail
    val currentCid = content.selection.cid
    val error = content.read.error?.messageOr("加载失败")
    val related = content.read.related
    videoDetailTail(detail, metaReady, currentCid, error, related, relatedEnter,
        textColor, surfaceColor, actions::switchPage,
        onRelated = { relatedItem ->
            VideoPageBus.pauseAll()
            navigateToVideo(context, relatedItem, fromVideoPage = true)
        })
}

@Composable
private fun VideoScreenComments(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    pager: androidx.compose.foundation.pager.PagerState, inlineLayout: VideoInlineLayout,
    ui: VideoDetailUiStateHolder, content: VideoScreenContent, paneWidth: Dp,
) {
    val detail = content.read.detail
    val fullscreen by ui.fullscreen
    val splitPanes = presentation.paneLayout.isSplit && !fullscreen && !presentation.pictureInPicture
    val statusBarTop = presentation.statusBarTop
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val immersiveStatusBar = presentation.immersiveStatusBar
    val initialCommentRoot = request.initialCommentRoot
    val initialCommentTarget = request.initialCommentTarget
    val d = detail
    if (d != null) {
        CommentScreen(
            request = CommentScreenRequest(
                aid = d.aid,
                upMid = d.ownerMid,
                title = d.title,
                bvid = d.bvid,
                focusRootRpid = initialCommentRoot,
                focusTargetRpid = initialCommentTarget,
            ),
            onBack = {},
            layout = CommentScreenLayout(
                bare = true,
                topInset = if (splitPanes) statusBarTop + 52.dp else playerBlockHeight + 14.dp +
                (if (immersiveStatusBar) 36.dp else 0.dp),
                trackBackProgress = pager.currentPage == 1,
                threadPlayerBottom = if (splitPanes) statusBarTop + 52.dp else playerBlockHeight,
                threadPaneWidth = if (splitPanes) paneWidth else null,
                playerFullscreen = fullscreen,
            ),
        )
    }
}
