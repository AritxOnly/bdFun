package com.aritxonly.bdfun.ui

data class HalfMoreState(
    val speed: Float,
    val autoPlayNext: Boolean,
    val loopMode: Int,
    val mirror: Boolean,
    val backgroundPlay: Boolean,
    val subtitlesOn: Boolean,
    val sleepAtMs: Long,
)

data class HalfMoreQuickActions(
    val onDislike: () -> Unit,
    val onWatchLater: () -> Unit,
    val onPictureInPicture: () -> Unit,
    val onCast: () -> Unit,
    val onCache: () -> Unit,
)

data class HalfMoreSettingsActions(
    val onSpeed: (Float) -> Unit,
    val onAutoPlayNext: () -> Unit,
    val onLoopMode: (Int) -> Unit,
    val onMirror: () -> Unit,
    val onBackgroundPlay: () -> Unit,
    val onSubtitlesToggle: () -> Unit,
    val onDanmakuSettings: () -> Unit,
    val onSleepAt: (Long) -> Unit,
)
