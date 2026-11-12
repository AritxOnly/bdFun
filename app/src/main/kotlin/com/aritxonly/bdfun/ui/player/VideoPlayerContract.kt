package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.*

internal data class VideoMediaInput(
    val bvid: String, val info: PlayInfo?, val quality: Quality?, val autoQuality: Boolean,
    val cover: String, val title: String, val author: String, val danmaku: List<DanmakuItem>,
)
internal data class VideoPlayerPresentation(
    val fullscreenInfo: FullscreenPlayerInfo, val fullscreen: Boolean,
    val resumePositionMs: Long, val immersiveStatusBar: Boolean, val pictureInPicture: Boolean,
)
internal class VideoTransportActions(
    val onPlayStateChanged: (Boolean) -> Unit, val onPageSelected: (VideoPage) -> Unit,
    val onSubtitleSelected: (SubtitleTrack?) -> Unit, val onQualitySelected: (Int) -> Unit,
    val onCacheVideo: (Quality) -> Unit,
)
internal class VideoInteractionActions(
    val onLikeClick: () -> Unit, val onDislikeClick: () -> Unit, val onCommentClick: () -> Unit,
    val onShareClick: () -> Unit, val onOpenSpaceClick: () -> Unit,
    val onToggleFolder: (Long) -> Unit, val onRequestFavorites: () -> Unit, val onAddWatchLater: () -> Unit,
)
internal class VideoNavigationActions(
    val onToggleFullscreen: () -> Unit, val onBackClick: () -> Unit, val onHomeClick: () -> Unit,
)
internal class VideoPlayerActions(
    val transport: VideoTransportActions, val interaction: VideoInteractionActions, val navigation: VideoNavigationActions,
)
