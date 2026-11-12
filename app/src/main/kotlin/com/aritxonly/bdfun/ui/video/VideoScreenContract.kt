package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem

internal data class VideoScreenRequest(
    val video: VideoItem,
    val defaultQuality: Int = 80,
    val openedFromVideoPage: Boolean = false,
    val initialCommentRoot: Long = 0L,
    val initialCommentTarget: Long = 0L,
    val initialSeekMs: Long = 0L,
)

internal data class VideoScreenPresentation(
    val paneLayout: VideoPaneLayout,
    val statusBarTop: Dp,
    val pictureInPicture: Boolean = false,
    val immersiveStatusBar: Boolean = false,
    val autoRotate: Boolean = true,
)

internal class VideoScreenEvents(
    val onOpenComments: (VideoDetail) -> Unit = {},
    val onBack: () -> Unit,
)
