package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackPhase
import kotlinx.coroutines.delay

@Composable
internal fun BindPlayerBuffering(playback: PlaybackController, ui: VideoPlayerUiStateHolder) {
    val state = playback.state.collectAsState()
    val buffering by remember(playback) {
        derivedStateOf { state.value.phase == PlaybackPhase.Buffering || state.value.phase == PlaybackPhase.Idle }
    }
    val ready by remember(playback) { derivedStateOf { state.value.ready } }
    var showBuffering by ui.showBuffering
    LaunchedEffect(buffering) {
        if (buffering) {
            showBuffering = false
            delay(if (ready) 1500L else 2000L)
            showBuffering = true
        } else {
            showBuffering = false
        }
    }
}
