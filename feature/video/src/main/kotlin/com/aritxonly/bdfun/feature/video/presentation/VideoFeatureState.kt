package com.aritxonly.bdfun.feature.video.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aritxonly.bdfun.feature.video.application.*

data class VideoFeatureState(
    val read: VideoDetailReadState,
    val selection: VideoPlaybackSelection,
    val interaction: VideoInteractionState,
    val metadata: VideoMetadataState,
)

@Composable
fun observeVideoFeatureState(
    detail: VideoDetailStateHolder,
    selection: VideoPlaybackSelectionStateHolder,
    interaction: VideoInteractionStateHolder,
    metadata: VideoMetadataStateHolder,
): VideoFeatureState {
    val read by detail.state.collectAsState()
    val selected by selection.state.collectAsState()
    val interacted by interaction.state.collectAsState()
    val meta by metadata.state.collectAsState()
    return VideoFeatureState(read, selected, interacted, meta)
}
