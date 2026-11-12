package com.aritxonly.bdfun.ui

internal data class InlineChromeState(
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedMs: Long,
    val scrubbing: Boolean,
    val seekLook: Float,
    val immersiveStatusBar: Boolean,
)

internal data class InlineChromeActions(
    val onBack: () -> Unit,
    val onHome: () -> Unit,
    val onRewind6: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onScrubStart: () -> Unit,
    val onScrubEnd: () -> Unit,
    val onScrubLook: (Float) -> Unit,
    val onFullscreen: () -> Unit,
    val onListen: () -> Unit,
    val onCast: () -> Unit,
    val onMore: () -> Unit,
)
