package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

@Composable
internal fun BindPlayerLoudness(playback: VideoPlaybackPlatform, ui: VideoPlayerUiStateHolder) {
    val settings = LocalVideoPageState.current.settings
    val state = playback.state.collectAsState()
    val ready by remember(playback) { derivedStateOf { state.value.ready } }
    var loudnessEnhancer by ui.loudnessEnhancer
    LaunchedEffect(ready, settings.loudnessEq) {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        if (ready && settings.loudnessEq) {
            loudnessEnhancer = playback.createLoudnessEffect()
        }
    }
    DisposableEffect(Unit) {
        onDispose { loudnessEnhancer?.release() }
    }
}
