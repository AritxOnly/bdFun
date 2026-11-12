package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.feature.video.application.*

internal data class VideoPlayerContent(
    val video: VideoItem, val detail: VideoDetail?, val playInfo: PlayInfo?, val quality: Quality?,
    val danmaku: List<DanmakuItem>, val selection: VideoPlaybackSelection,
    val interaction: VideoInteractionState, val metadata: VideoMetadataState, val loggedIn: Boolean,
)
internal data class VideoPlayerFlags(val fullscreen: Boolean, val immersiveStatusBar: Boolean, val pictureInPicture: Boolean)
internal data class VideoPlayerAssembly(val media: VideoMediaInput, val presentation: VideoPlayerPresentation)

/** 将应用状态投影为现有播放器入参，不改变封面/标题回退与自动清晰度文案。 */
internal fun assembleVideoPlayer(content: VideoPlayerContent, flags: VideoPlayerFlags): VideoPlayerAssembly {
    val detail = content.detail
    val video = content.video
    val media = VideoMediaInput(content.selection.bvid, content.playInfo, content.quality, content.selection.autoQuality,
        detail?.cover ?: video.cover, detail?.title ?: video.title, detail?.ownerName ?: video.ownerName, content.danmaku)
    return VideoPlayerAssembly(media, VideoPlayerPresentation(
        fullscreenInfo = fullscreenPlayerInfo(content), fullscreen = flags.fullscreen,
        resumePositionMs = content.selection.resumePositionMs, immersiveStatusBar = flags.immersiveStatusBar,
        pictureInPicture = flags.pictureInPicture,
    ))
}

private fun fullscreenPlayerInfo(content: VideoPlayerContent): FullscreenPlayerInfo {
    val detail = content.detail
    val video = content.video
    val state = content.interaction
    val metadata = content.metadata
    return FullscreenPlayerInfo(
        title = detail?.title ?: video.title, author = detail?.ownerName ?: video.ownerName,
        authorFace = detail?.ownerFace ?: video.ownerFace,
        onlineText = metadata.onlineText, onlineCount = metadata.onlineCount,
        followed = state.followed, liked = state.liked, likeCount = state.likeCount,
        replyCount = detail?.reply ?: 0L, favorited = state.favorited, favoriteCount = state.favoriteCount,
        shareCount = state.shareCount,
        qualityLabel = if (content.selection.autoQuality) "自动" else content.quality?.label ?: "",
        cover = detail?.cover ?: video.cover, pages = detail?.let(com.aritxonly.bdfun.core.domain.VideoPagePolicy::pages).orEmpty(),
        currentCid = content.selection.cid, folders = state.folders, folderIds = state.folderIds,
        loggedIn = content.loggedIn, subtitles = metadata.subtitles, selectedSubtitle = metadata.selectedSubtitle,
        subtitleCues = metadata.subtitleCues, danmakuItems = content.danmaku,
    )
}
