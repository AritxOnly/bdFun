package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToSpace

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoScreenPager(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    pager: androidx.compose.foundation.pager.PagerState, inlineLayout: VideoInlineLayout,
    pageUi: VideoDetailUiStateHolder, description: VideoDescriptionStateHolder,
    content: VideoScreenContent, actions: VideoDetailViewActions, relatedEnter: Float,
) {
    val page = LocalVideoPageState.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val renderActions = VideoDetailRenderActions(
        VideoMetadataActions(actions::toggleFollow, actions::toggleLike, actions::toggleDislike,
            actions::giveCoin, actions::toggleFavorite, actions::shareVideo, actions::tripleAction,
            onOpenSpace = {
                page.pauseAll()
                navigateToSpace(context, content.read.detail?.ownerMid ?: content.video.ownerMid)
            }),
        onPage = actions::switchPage,
        onRelated = { page.pauseAll(); navigateToVideo(context, it, fromVideoPage = true) },
    )
    VideoDetailContentPager(request, presentation, pager, inlineLayout, pageUi, description,
        content, renderActions, relatedEnter, VideoDetailPagerSlots(
            metadata = {
                val metadata = rememberVideoMetadataAssembly()
                VideoMetadataVisuals(metadata.appearance, metadata.slots)
            },
            tail = rememberVideoDetailTailSlots(), comments = { VideoScreenComments(it) },
        ))
}

@Composable
private fun VideoScreenComments(pane: VideoCommentPane) {
    val d = pane.detail
    CommentScreen(
        request = CommentScreenRequest(aid = d.aid, upMid = d.ownerMid, title = d.title, bvid = d.bvid,
            focusRootRpid = pane.root, focusTargetRpid = pane.target),
        onBack = {},
        layout = CommentScreenLayout(bare = true, topInset = pane.topInset,
            trackBackProgress = pane.trackBackProgress, threadPlayerBottom = pane.threadPlayerBottom,
            threadPaneWidth = pane.threadPaneWidth, playerFullscreen = pane.fullscreen),
    )
}
