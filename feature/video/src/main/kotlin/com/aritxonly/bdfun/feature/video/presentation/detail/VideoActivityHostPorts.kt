package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.VideoItem

/** Activity 只暴露原生窗口/导航动作，Compose 内容不持有生命周期实现。 */
class VideoActivityHostActions(
    val onBack: () -> Unit, val finish: () -> Unit,
    val applyStatusBarStyle: (Boolean) -> Unit, val resyncSystemBars: () -> Unit,
    val syncSystemBars: (Boolean) -> Unit, val openVideo: (VideoItem) -> Unit,
    val openedFromVideoPage: () -> Boolean, val hasWindow: () -> Boolean,
)
