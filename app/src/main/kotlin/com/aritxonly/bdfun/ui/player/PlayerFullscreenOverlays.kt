package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.ui.FullscreenTransport

import com.aritxonly.bdfun.ui.FullscreenPlaybackOptions




import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
internal fun PlayerFullscreenOverlay(
    media: VideoMediaInput,
    presentation: VideoPlayerPresentation,
    ui: VideoPlayerUiStateHolder,
    fsActions: FullscreenActions,
    playing: Boolean, positionMs: Long, durationMs: Long, bufferedMs: Long,
) {
    val PlayerUiState = LocalVideoPageState.current.playerUi
    val fullscreen = presentation.fullscreen
    val pictureInPicture = presentation.pictureInPicture
    val fullscreenInfo = presentation.fullscreenInfo
    var controlsVisible by ui.controlsVisible
    var fsInputOpen by ui.fsInputOpen
    var fsPanel by ui.fsPanel
    var fsScrubbing by ui.fsScrubbing
    var fsSeekLook by ui.fsSeekLook
    var fsBarPreviewMs by ui.fsBarPreviewMs
    val controlsAlpha = if (controlsVisible && !pictureInPicture) 1f else 0f
    if (fullscreen && !pictureInPicture) {
        FullscreenPlayerChrome(
            info = fullscreenInfo,
            playback = fullscreenPlaybackProjection(media, ui, playing, positionMs, durationMs, bufferedMs, LocalVideoPageState.current.settings),
            actions = fsActions,
            panel = fsPanel,
            onPanelChanged = { fsPanel = it },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .offset(x = if (controlsVisible && !fsInputOpen) 0.dp else 10_000.dp)
                .graphicsLayer { alpha = if (fsInputOpen) 0f else controlsAlpha },
            scrubOnly = fsScrubbing,
            onScrubStart = { fsScrubbing = true },
            onScrubEnd = {
                fsScrubbing = false
                fsSeekLook = 0f
            },
            onScrubLook = { fsSeekLook = it },
            onScrubPreview = { fsBarPreviewMs = it },
            onPanelHidden = {
                controlsVisible = false
                PlayerUiState.controlsVisible = false
            },
        )
    }

}

private fun fullscreenPlaybackProjection(
    media: VideoMediaInput, ui: VideoPlayerUiStateHolder,
    playing: Boolean, positionMs: Long, durationMs: Long, bufferedMs: Long,
    settings: com.aritxonly.bdfun.core.domain.SettingsRepository,
): FullscreenPlaybackState {
    val info = media.info
    val quality = media.quality
    val autoQuality = media.autoQuality
    val controlsVisible by ui.controlsVisible
    val seekPreviewMs by ui.seekPreviewMs
    val speed by ui.speed
    val fsAutoPlayNext by ui.fsAutoPlayNext
    val fsLoopMode by ui.fsLoopMode
    val fsMirror by ui.fsMirror
    val fsResizeMode by ui.fsResizeMode
    val fsColorAssist by ui.fsColorAssist
    val fsAudioOnly by ui.fsAudioOnly
    val fsSleepAt by ui.fsSleepAt
    val fsSeekLook by ui.fsSeekLook
    val fsScrubbing by ui.fsScrubbing
    return FullscreenPlaybackState(
        transport = FullscreenTransport(
            playing = playing,
            positionMs = if (controlsVisible) {
                    if (seekPreviewMs >= 0L) seekPreviewMs else positionMs
                } else {
                    0L
                },
            durationMs = if (controlsVisible) durationMs else 0L,
            bufferedMs = if (controlsVisible) bufferedMs else 0L,
            speed = speed,
            seekLook = if (controlsVisible) fsSeekLook else 0f,
            scrubbing = controlsVisible && fsScrubbing,
        ),
        options = FullscreenPlaybackOptions(
            qualities = info?.qualities.orEmpty(),
            qualityId = quality?.id ?: 0,
            autoQuality = autoQuality,
            danmakuEnabled = settings.danmakuOn,
            autoPlayNext = fsAutoPlayNext,
            loopMode = fsLoopMode,
            mirror = fsMirror,
            resizeMode = fsResizeMode,
            colorAssist = fsColorAssist,
            audioOnly = fsAudioOnly,
            sleepAtMs = fsSleepAt,
            backgroundPlay = settings.backgroundPlay,
        ),
    )
}

@Composable
internal fun BoxScope.PlayerLockOverlay(ui: VideoPlayerUiStateHolder, fullscreen: Boolean) {
    val PlayerUiState = LocalVideoPageState.current.playerUi
    var fsLocked by ui.fsLocked
    var fsLockHint by ui.fsLockHint
    var controlsVisible by ui.controlsVisible
    var fsPanel by ui.fsPanel
    var fsInputOpen by ui.fsInputOpen
    var qualityMenu by ui.qualityMenu
    var speedMenu by ui.speedMenu
    // 全屏锁：未锁定时跟控件一起出现；锁定后点画面才短暂显示锁图标。
    if (fullscreen && fsLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(15f)
                .pointerInput(Unit) {
                    detectTapGestures {
                        fsLockHint = true
                    }
                },
        )
    }
    if (fullscreen && ((fsLocked && fsLockHint) ||
            (!fsLocked && controlsVisible && fsPanel == FullscreenPanel.None && !fsInputOpen))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(50.dp)
                .zIndex(20f)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.50f))
                .clickable(interactionSource = null, indication = null) {
                    fsLocked = !fsLocked
                    fsLockHint = false
                    if (fsLocked) {
                        controlsVisible = false
                        PlayerUiState.controlsVisible = false
                        fsPanel = FullscreenPanel.None
                        fsInputOpen = false
                        qualityMenu = false
                        speedMenu = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            LockGlyph(locked = fsLocked)
        }
    }

}

@Composable
internal fun BoxScope.PlayerScrubOverlay(
    ui: VideoPlayerUiStateHolder, fullscreen: Boolean,
    positionMs: Long, durationMs: Long, bufferedMs: Long,
) {
    val fsScrubbing by ui.fsScrubbing
    val fsBarPreviewMs by ui.fsBarPreviewMs
    val seekPreviewMs by ui.seekPreviewMs
    val fsSeekLook by ui.fsSeekLook
    if (fullscreen && fsScrubbing) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = if (fullscreen) 52.dp else 4.dp,
                    end = if (fullscreen) 42.dp else 4.dp,
                    bottom = if (fullscreen) 52.dp else 28.dp,
                )
                .zIndex(11f),
        ) {
            FullscreenSeekBar(
                positionMs = when {
                    fsBarPreviewMs >= 0L -> fsBarPreviewMs
                    seekPreviewMs >= 0L -> seekPreviewMs
                    else -> positionMs
                },
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                onInteract = {},
                onSeek = {},
                modifier = Modifier.fillMaxWidth(),
                externalPressed = true,
                externalLook = fsSeekLook,
            )
        }
    }


}

@Composable
internal fun PlayerSubtitleLayer(
    fullscreenInfo: FullscreenPlayerInfo, fullscreen: Boolean,
    ui: VideoPlayerUiStateHolder, positionMs: Long,
) {
    val fsAudioOnly by ui.fsAudioOnly
    val fsSubtitlesOn by ui.fsSubtitlesOn
    val fsPanel by ui.fsPanel
    val controlsVisible by ui.controlsVisible
    // 字幕层：控件自动隐藏后仍然保留当前句
    if (!fsAudioOnly && fsSubtitlesOn && fsPanel == FullscreenPanel.None &&
        !fullscreenInfo.subtitleCues.isEmpty()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SubtitleOverlay(
                cues = fullscreenInfo.subtitleCues,
                positionMs = positionMs,
                // 控件打开时字幕抬到进度条上方，控件收起时贴到底部。
                bottomPadding = if (controlsVisible) {
                    if (fullscreen) 116.dp else 52.dp
                } else {
                    if (fullscreen) 24.dp else 14.dp
                },
            )
        }
    }

}
