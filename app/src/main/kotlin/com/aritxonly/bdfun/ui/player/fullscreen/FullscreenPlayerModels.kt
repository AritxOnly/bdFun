package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.data.FavoriteFolder
import com.aritxonly.bdfun.data.DanmakuItem
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.bdfun.data.SubtitleCue
import com.aritxonly.bdfun.data.SubtitleTrack
import com.aritxonly.bdfun.data.VideoPage

internal enum class FullscreenPanel {
    None,
    Favorite,
    More,
    Danmaku,
    Episodes,
    Speed,
    Quality,
    Subtitles,
    Cache,
}

internal val FullscreenSpeeds = listOf(2f, 1.5f, 1.25f, 1f, 0.75f, 0.5f)

internal data class FullscreenPlayerInfo(
    val title: String,
    val author: String,
    val authorFace: String,
    val onlineText: String,
    val onlineCount: Long,
    val followed: Boolean = false,
    val liked: Boolean,
    val likeCount: Long,
    val replyCount: Long,
    val favorited: Boolean,
    val favoriteCount: Long,
    val shareCount: Long,
    val qualityLabel: String,
    val cover: String,
    val pages: List<VideoPage>,
    val currentCid: Long,
    val folders: List<FavoriteFolder>,
    val folderIds: Set<Long>,
    val loggedIn: Boolean,
    val subtitles: List<SubtitleTrack>,
    val selectedSubtitle: SubtitleTrack?,
    val subtitleCues: List<SubtitleCue>,
    val danmakuItems: List<DanmakuItem> = emptyList(),
)

internal data class FullscreenPlaybackState(
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedMs: Long = 0L,
    val speed: Float,
    val qualities: List<Quality>,
    val qualityId: Int,
    val autoQuality: Boolean = false,
    val danmakuEnabled: Boolean,
    val autoPlayNext: Boolean,
    val loopMode: Int,
    val mirror: Boolean,
    val resizeMode: Int,
    val colorAssist: Int,
    val audioOnly: Boolean,
    val sleepAtMs: Long,
    val backgroundPlay: Boolean,
    val seekLook: Float = 0f,
    val scrubbing: Boolean = false,
)

internal class FullscreenActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onEpisodes: () -> Unit,
    val onLike: () -> Unit,
    val onComment: () -> Unit,
    val onShare: () -> Unit,
    val onOpenSpace: () -> Unit,
    val onToggleFolder: (Long) -> Unit,
    val onRequestFavorites: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onQualitySelected: (Int) -> Unit,
    val onOpenCachePanel: () -> Unit,
    val onCacheSelected: (Quality) -> Unit,
    val onSpeedSelected: (Float) -> Unit,
    val onPageSelected: (VideoPage) -> Unit,
    val onSubtitleSelected: (SubtitleTrack?) -> Unit,
    val onSendDanmaku: () -> Unit,
    val onToggleDanmaku: () -> Unit,
    val onAddWatchLater: () -> Unit,
    val onPictureInPicture: () -> Unit,
    val onCast: () -> Unit,
    val onAudioOnlyChanged: (Boolean) -> Unit,
    val onAutoPlayNextChanged: (Boolean) -> Unit,
    val onLoopModeChanged: (Int) -> Unit,
    val onMirrorChanged: (Boolean) -> Unit,
    val onResizeModeChanged: (Int) -> Unit,
    val onColorAssistChanged: (Int) -> Unit,
    val onSleepAtChanged: (Long) -> Unit,
    val onBackgroundPlayChanged: (Boolean) -> Unit,
    val onSeekRelative: (Long) -> Unit,
)
