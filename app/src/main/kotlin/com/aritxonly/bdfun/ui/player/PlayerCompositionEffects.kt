package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform
import com.aritxonly.bdfun.feature.video.application.PlaybackTrackingStateHolder

@Composable
internal fun BindPlayerInitialEffects(
    playback: VideoPlaybackPlatform, media: VideoMediaInput, ui: VideoPlayerUiStateHolder,
) {
    val bvid = media.bvid
    val info = media.info
    val quality = media.quality
    var usingFallback by ui.usingFallback
    var usingOriginal by ui.usingOriginal
    BindPlayerLifecycle(playback, ui)
    BindPlayerBuffering(playback, ui)
    LaunchedEffect(info, quality) {
        usingFallback = false
        usingOriginal = false
        com.aritxonly.bdfun.data.LogCollector.record(
            "PLAYER",
            "quality-change bvid=$bvid q=${quality?.id} hasInfo=${info != null} " +
                "qualities=${info?.qualities?.size ?: 0}",
        )
    }
    BindPlayerLoudness(playback, ui)
}

@Composable
internal fun BindPlayerPresentationSetup(
    media: VideoMediaInput, presentation: VideoPlayerPresentation, ui: VideoPlayerUiStateHolder,
) {
    val settings = LocalVideoPageState.current.settings
    val bvid = media.bvid
    val fullscreenInfo = presentation.fullscreenInfo
    val fullscreen = presentation.fullscreen
    var speedMenu by ui.speedMenu
    val speedSpring = rememberSpringFloat(if (speedMenu) 1f else 0f, stiffness = 300f, damping = 22f)
    var qualityMenu by ui.qualityMenu
    val qualitySpring =
        rememberSpringFloat(if (qualityMenu) 1f else 0f, stiffness = 300f, damping = 22f)
    // 全屏新 UI 的本地状态：二级面板、听视频、镜像、循环、画面尺寸、定时关闭等
    BindPlayerOrientation(fullscreen, ui)
    var fsAutoPlayNext by ui.fsAutoPlayNext
    LaunchedEffect(fsAutoPlayNext) {
        settings.autoPlayNext = fsAutoPlayNext
    }
    var videoScale by ui.videoScale
    var videoRotation by ui.videoRotation
    var videoOffset by ui.videoOffset
    LaunchedEffect(bvid, fullscreenInfo.currentCid) {
        videoScale = 1f
        videoRotation = 0f
        videoOffset = Offset.Zero
    }

}

@Composable
internal fun BindPlayerFrameResources(media: VideoMediaInput, userAgent: String) {
    val bvid = media.bvid
    val quality = media.quality
    val info = media.info
    // 官方 videoshot 精灵图（首帧预览取不到时用它）
    val frameLoader = remember { VideoFrameLoader() }
    val frameScope = rememberCoroutineScope()
    val frameUrl = quality?.videoUrl ?: info?.fallbackUrl
    val frameHeaders = remember(bvid) {
        mapOf(
            "User-Agent" to userAgent,
            "Referer" to "https://www.bilibili.com/video/$bvid/",
        )
    }

    DisposableEffect(Unit) {
        onDispose { frameLoader.release() }
    }

}

@Composable
internal fun BindPlayerEngineEffects(
    playback: VideoPlaybackPlatform, observed: PlayerObservedStateHolder,
    media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, ui: VideoPlayerUiStateHolder, tracking: PlaybackTrackingStateHolder,
) {
    val playing by observed.playing
    BindPlayerMediaBridge(playback, media, presentation, actions, ui)
    BindPlayerControlVisibility(playback, presentation.fullscreen, playing, ui)
    BindPlayerObserver(playback, media, actions, ui)
    BindPlayerSource(playback, media, presentation, ui)
    BindPlayerProgressTracking(playback, media.bvid, ui, tracking)
}

@Composable
internal fun rememberPlayerBluetoothLatency(): Int {
    val settings = LocalVideoPageState.current.settings
    val connected = rememberBluetoothAudioConnected()
    return if (settings.bluetoothLatencyEnabled && connected) settings.bluetoothLatencyMs else 0
}
