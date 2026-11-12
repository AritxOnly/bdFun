package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

@Composable
internal fun PlayerLayout(
    playback: VideoPlaybackPlatform, observed: PlayerObservedStateHolder,
    media: VideoMediaInput, presentation: VideoPlayerPresentation, actions: VideoPlayerActions,
    ui: VideoPlayerUiStateHolder, danmakuVisible: Boolean, btLatencyMs: Int,
    sponsor: PlayerSponsorPresentation, modifier: Modifier,
) {
    val ports = rememberPlayerRenderPorts(playback, media, presentation, actions, ui)
    VideoPlayerLayout(playback, observed, media, presentation, actions, ui,
        danmakuVisible, btLatencyMs, sponsor, modifier, ports)
}
