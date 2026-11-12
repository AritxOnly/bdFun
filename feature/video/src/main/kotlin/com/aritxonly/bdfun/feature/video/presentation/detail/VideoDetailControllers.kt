package com.aritxonly.bdfun.ui

data class VideoDetailControllers(
    val detail: com.aritxonly.bdfun.feature.video.application.VideoDetailStateHolder,
    val interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionStateHolder,
    val selection: com.aritxonly.bdfun.feature.video.application.VideoPlaybackSelectionStateHolder,
    val readCache: com.aritxonly.bdfun.feature.video.application.VideoReadCacheScope,
)
