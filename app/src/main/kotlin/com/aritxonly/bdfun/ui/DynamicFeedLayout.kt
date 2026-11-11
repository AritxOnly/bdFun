package com.aritxonly.bdfun.ui

internal const val DynamicCardMinWidth = 280
internal const val DynamicCardMaxWidth = 560

internal fun dynamicSidebarWidth(viewportWidthDp: Float): Float =
    if (viewportWidthDp < 736f) 0f else (viewportWidthDp * 0.2f).coerceIn(148f, 196f)
