package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.core.model.video.VideoItem

/** 共享视频行也用于空间页，其原转场/图片策略由app提供，避免feature间互相依赖。 */
class VideoDetailTailSlots(
    val cover: @Composable (String, Modifier) -> Unit,
    val relatedRow: @Composable (VideoItem, () -> Unit) -> Unit,
)
