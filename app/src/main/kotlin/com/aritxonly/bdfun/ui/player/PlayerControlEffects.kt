package com.aritxonly.bdfun.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform
import kotlinx.coroutines.delay

@Composable
internal fun BindPlayerControlVisibility(
    playback: VideoPlaybackPlatform,
    fullscreen: Boolean,
    playing: Boolean,
    ui: VideoPlayerUiStateHolder,
) {
    val context = LocalContext.current
    val PlayerUiState = LocalVideoPageState.current.playerUi
    var controlsVisible by ui.controlsVisible
    var speedMenu by ui.speedMenu
    var fsPanel by ui.fsPanel
    var fsInputOpen by ui.fsInputOpen
    var fsAudioOnly by ui.fsAudioOnly
    var lastFullscreenForControls by ui.lastFullscreenForControls
    DisposableEffect(playing) {
        val act = context as? Activity
        if (playing) {
            act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(controlsVisible) { PlayerUiState.controlsVisible = controlsVisible }
    DisposableEffect(Unit) {
        onDispose { PlayerUiState.controlsVisible = false }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            PlayerUiState.danmakuPanelOpen = false
            speedMenu = false
            fsPanel = FullscreenPanel.None
            fsInputOpen = false
        }
    }

    LaunchedEffect(fullscreen) {
        if (fullscreen == lastFullscreenForControls) return@LaunchedEffect
        lastFullscreenForControls = fullscreen
        // 进入 / 退出全屏（包括旋转触发）都不自动弹出控件，全部先收起。
        controlsVisible = false
        PlayerUiState.controlsVisible = false
        if (!fullscreen) {
            fsPanel = FullscreenPanel.None
            fsInputOpen = false
            fsAudioOnly = false
        }
    }

    LaunchedEffect(fsAudioOnly) {
        if (fsAudioOnly) {
            controlsVisible = true
            PlayerUiState.controlsVisible = true
        }
        // 听视频：真正关掉视频轨，只保留音频解码，不是拿一张图盖住画面
        playback.setVideoEnabled(!fsAudioOnly)
    }

    BindPlayerControlsAutoHide(ui)
}

@Composable
private fun BindPlayerControlsAutoHide(ui: VideoPlayerUiStateHolder) {
    val PlayerUiState = LocalVideoPageState.current.playerUi
    var controlsVisible by ui.controlsVisible
    val interactionTick by ui.interactionTick
    val boosting by ui.boosting
    val fsPanel by ui.fsPanel
    val fsInputOpen by ui.fsInputOpen
    val fsAudioOnly by ui.fsAudioOnly
    val fsScrubbing by ui.fsScrubbing
    LaunchedEffect(
        controlsVisible,
        interactionTick,
        PlayerUiState.danmakuPanelOpen,
        PlayerUiState.danmakuInteract,
        boosting,
        fsPanel,
        fsInputOpen,
        fsAudioOnly,
        fsScrubbing,
    ) {
        if (controlsVisible && !PlayerUiState.danmakuPanelOpen && !boosting &&
            fsPanel == FullscreenPanel.None && !fsInputOpen && !fsAudioOnly &&
            !fsScrubbing
        ) {
            delay(HIDE_DELAY_MS)
            controlsVisible = false
            PlayerUiState.controlsVisible = false
        }
    }

}
