package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.video.Quality
import com.aritxonly.bdfun.feature.video.application.*

data class VideoScreenContent(
    val video: VideoItem,
    val read: VideoDetailReadState,
    val selection: VideoPlaybackSelection,
    val interaction: VideoInteractionState,
    val metadata: VideoMetadataState,
    val quality: Quality?,
    val loggedIn: Boolean,
)
