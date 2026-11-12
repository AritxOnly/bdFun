package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// 高倍速放最上面。
internal val SPEEDS = listOf(2f, 1.5f, 1.25f, 1f, 0.75f, 0.5f)

internal val TopGradientFullscreen = 120.dp
internal val TopGradientInline = 64.dp
internal val BottomGradientFullscreen = 160.dp
internal val BottomGradientInline = 92.dp

@Composable
internal fun VideoPlayer(
    media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, modifier: Modifier = Modifier,
) {
    val dependencies = LocalVideoPageState.current.dependencies
    val settings = LocalVideoPageState.current.settings
    val context = LocalContext.current
    val ui = remember {
        VideoPlayerUiStateHolder(settings.defaultPlaybackSpeed, settings.autoPlayNext, presentation.fullscreen)
    }
    val playback = remember {
        dependencies.createPlayback(context, runCatching { settings.decoderMode }.getOrDefault(0))
    }
    val observed = rememberPlayerObservedState(playback)
    BindPlayerInitialEffects(playback, media, ui)
    val tracking = remember { dependencies.controllers.tracking() }
    val sponsor = rememberPlayerSponsor(media.bvid, tracking, observed)
    BindPlayerPresentationSetup(media, presentation, ui)
    val latencyMs = rememberPlayerBluetoothLatency()
    BindPlayerContinuation(playback, presentation.fullscreenInfo, actions.transport.onPageSelected, ui,
        LocalVideoPageState.current, dependencies.queue::hasNext, rememberPlayerDiagnostics().toast)
    BindPlayerFrameResources(media, playback.userAgent)
    BindPlayerEngineEffects(playback, observed, media, presentation, actions, ui, tracking)
    val audioOnly by ui.fsAudioOnly
    val danmakuVisible = !audioOnly && settings.danmakuOn && media.danmaku.isNotEmpty()
    val playing by observed.playing
    BindPlayerHotDanmaku(playback, media.danmaku, danmakuVisible, ui)
    BindPlayerDanmakuClock(playback, playing, danmakuVisible, ui)
    BindPlayerSystemBars(presentation.fullscreen, presentation.immersiveStatusBar, ui)
    BindPlayerHdrWindow(media.quality, presentation.fullscreen)
    PlayerLayout(playback, observed, media, presentation, actions, ui, danmakuVisible, latencyMs, sponsor, modifier)
}
