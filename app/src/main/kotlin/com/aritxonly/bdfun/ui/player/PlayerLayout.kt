package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

@Composable
internal fun PlayerLayout(
    playback: VideoPlaybackPlatform, observed: PlayerObservedStateHolder,
    media: VideoMediaInput, presentation: VideoPlayerPresentation, actions: VideoPlayerActions,
    ui: VideoPlayerUiStateHolder, danmakuVisible: Boolean, btLatencyMs: Int,
    sponsor: PlayerSponsorPresentation, modifier: Modifier,
) {
    val context = LocalContext.current
    val VideoPageBus = LocalVideoPageState.current
    val fullscreen = presentation.fullscreen
    val fullscreenInfo = presentation.fullscreenInfo
    val danmaku = media.danmaku
    val onToggleFullscreen = actions.navigation.onToggleFullscreen
    val playing by observed.playing
    val ended by observed.ended
    val positionMs by observed.positionMs
    val durationMs by observed.durationMs
    val bufferedMs by observed.bufferedMs
    Box(
        modifier = modifier
            .background(Color.Black)
            .then(playerInteractionModifier(playback, ui, fullscreen, VideoPageBus, onToggleFullscreen)),
    ) {
        PlayerSurface(
            playback, media, presentation, ui, danmakuVisible, btLatencyMs,
            sponsor.active,
        ) { segment ->
            playback.seekTo(segment.endMs)
            sponsor.dismiss(segment)
        }

        PlayerStatusOverlays(ui, fullscreen, ended, durationMs)

        BindPlayerOverlayBackHandling(ui, fullscreen)

        val fsActions = VideoFullscreenActionBindings(
            context, playback, fullscreenInfo, actions, ui, VideoPageBus,
        ).create()

        PlayerZoomResetOverlay(ui, fullscreen)

        PlayerFullscreenOverlay(
            media, presentation, ui, fsActions, playing, positionMs, durationMs, bufferedMs,
        )

        PlayerLockOverlay(ui, fullscreen)

        PlayerScrubOverlay(ui, fullscreen, positionMs, durationMs, bufferedMs)

        PlayerSubtitleLayer(fullscreenInfo, fullscreen, ui, positionMs)

        PlayerInlineControls(playback, observed, presentation, actions, fsActions, ui)
        PlayerInlineAudio(playback, observed, presentation, ui)

        PlayerDanmakuInputOverlay(fullscreen, danmaku, ui)

        PlayerInlineMore(playback, media, presentation, actions, fsActions, ui)
    }
}

@Composable
private fun BindPlayerOverlayBackHandling(ui: VideoPlayerUiStateHolder, fullscreen: Boolean) {
    val controlsVisible by ui.controlsVisible
    var qualityMenu by ui.qualityMenu
    var speedMenu by ui.speedMenu
    var fsLocked by ui.fsLocked
    var fsPanel by ui.fsPanel
    var fsInputOpen by ui.fsInputOpen
    // 控件隐藏时画质/倍速面板一起收回（之前画质面板会一直挂着）
    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            qualityMenu = false
            speedMenu = false
        }
    }

    BackHandler(enabled = fullscreen && fsLocked) {
        // 锁屏时返回键只解锁，不退出全屏。
        fsLocked = false
    }

    BackHandler(enabled = fullscreen && (fsPanel != FullscreenPanel.None || fsInputOpen)) {
        if (fsInputOpen) {
            fsInputOpen = false
        } else {
            fsPanel = FullscreenPanel.None
        }
    }

}
