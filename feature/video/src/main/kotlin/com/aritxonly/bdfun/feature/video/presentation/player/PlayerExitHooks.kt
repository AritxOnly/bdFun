package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.PlaybackController

/** 设置在退出事件中读取；原生交接继续由app捕获当前页面Context装配。 */
class PlayerExitHooks(
    val resumePlayback: () -> Boolean,
    val showDiscAfterVideo: () -> Boolean,
    val stopOnVideoPageExit: () -> Boolean,
    val shouldHandOff: () -> Boolean,
    val adopt: (PlaybackController) -> Unit,
    val cancelHandOff: () -> Unit,
)
