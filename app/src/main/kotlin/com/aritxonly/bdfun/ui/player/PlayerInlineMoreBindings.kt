package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.data.Quality

@Composable
internal fun PlayerInlineMore(
    playback: PlaybackController, media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, fsActions: FullscreenActions, ui: VideoPlayerUiStateHolder,
) {
    val settings = LocalVideoPageState.current.settings
    val context = LocalContext.current
    val VideoPageBus = LocalVideoPageState.current
    val fullscreen = presentation.fullscreen
    var halfMoreOpen by ui.halfMoreOpen
    var speed by ui.speed
    var fsAutoPlayNext by ui.fsAutoPlayNext
    var fsLoopMode by ui.fsLoopMode
    var fsMirror by ui.fsMirror
    var fsSubtitlesOn by ui.fsSubtitlesOn
    var fsSleepAt by ui.fsSleepAt
    // 半屏「更多」：与全屏 MorePanel 同一套设置项，复用评论区同款底部弹簧面板。
    if (!fullscreen) {
        HalfScreenMoreSheet(
            visible = halfMoreOpen,
            onDismiss = { halfMoreOpen = false },
            state = HalfMoreState(
                speed = speed,
                autoPlayNext = fsAutoPlayNext,
                loopMode = fsLoopMode,
                mirror = fsMirror,
                backgroundPlay = settings.backgroundPlay,
                subtitlesOn = fsSubtitlesOn,
                sleepAtMs = fsSleepAt,
            ),
            quick = inlineMoreQuickActions(context, media.quality, actions, fsActions, ui),
            settings = inlineMoreSettingsActions(playback, ui, VideoPageBus, VideoPageBus.settings),
        )
    }
}

private fun inlineMoreQuickActions(
    context: Context, quality: Quality?, actions: VideoPlayerActions,
    fsActions: FullscreenActions, ui: VideoPlayerUiStateHolder,
): HalfMoreQuickActions {
    var halfMoreOpen by ui.halfMoreOpen
    val onDislikeClick = actions.interaction.onDislikeClick
    val onAddWatchLater = actions.interaction.onAddWatchLater
    val onCacheVideo = actions.transport.onCacheVideo
    return HalfMoreQuickActions(
        onDislike = {
            onDislikeClick()
            context.showToast("已减少此类推荐")
        },
        onWatchLater = {
            onAddWatchLater()
            halfMoreOpen = false
        },
        onPictureInPicture = {
            fsActions.onPictureInPicture()
            halfMoreOpen = false
        },
        onCast = {
            fsActions.onCast()
            halfMoreOpen = false
        },
        onCache = {
            val item = quality
            if (item == null) {
                context.showToast("暂无可用清晰度")
            } else {
                onCacheVideo(item)
            }
            halfMoreOpen = false
        },
    )
}

private fun inlineMoreSettingsActions(
    playback: PlaybackController, ui: VideoPlayerUiStateHolder, VideoPageBus: VideoPagePresentationState,
    settings: com.aritxonly.bdfun.core.domain.SettingsRepository,
): HalfMoreSettingsActions {
    val PlayerUiState = VideoPageBus.playerUi
    var speed by ui.speed
    var fsAutoPlayNext by ui.fsAutoPlayNext
    var fsLoopMode by ui.fsLoopMode
    var fsMirror by ui.fsMirror
    var fsSubtitlesOn by ui.fsSubtitlesOn
    var fsSleepAt by ui.fsSleepAt
    return HalfMoreSettingsActions(
        onSpeed = {
            speed = it
            playback.setSpeed(it)
        },
        onAutoPlayNext = { fsAutoPlayNext = !fsAutoPlayNext },
        onLoopMode = { fsLoopMode = it },
        onMirror = { fsMirror = !fsMirror },
        onBackgroundPlay = {
            settings.backgroundPlay = !settings.backgroundPlay
        },
        onSubtitlesToggle = { fsSubtitlesOn = !fsSubtitlesOn },
        onDanmakuSettings = {
            if (!PlayerUiState.danmakuPanelOpen) {
                VideoPageBus.panelBackdrop = grabPlayerVideoFrame(ui)
            }
            PlayerUiState.danmakuPanelOpen = true
        },
        onSleepAt = { target ->
            fsSleepAt = target
        },
    )
}

private fun grabPlayerVideoFrame(ui: VideoPlayerUiStateHolder): androidx.compose.ui.graphics.ImageBitmap? {
        val bmp = ui.playerViewRef.value?.captureThirdSize() ?: return null
        return runCatching { bmp.asImageBitmap() }.getOrNull()
    }
