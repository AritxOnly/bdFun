package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.feed.DynamicItem
import com.aritxonly.bdfun.core.model.video.VideoItem

internal data class DynamicCardActions(
    val onOpen: () -> Unit,
    val onOpenVideo: (VideoItem) -> Unit,
    val onLike: () -> Unit,
    val onComment: () -> Unit,
    val onRepost: () -> Unit,
    val onOpenForwarded: (DynamicItem) -> Unit = {},
    val onToggleMute: () -> Unit = {},
)

internal data class DynamicCardPresentation(
    val liked: Boolean,
    val autoPlay: Boolean = false,
    val muted: Boolean = true,
    val expanded: Boolean = false,
    val animatedOpen: Boolean = true,
)
