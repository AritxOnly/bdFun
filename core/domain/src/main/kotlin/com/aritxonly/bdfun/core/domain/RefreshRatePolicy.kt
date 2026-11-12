package com.aritxonly.bdfun.core.domain

/** 保留 1500ms 静止门槛和 0/1/60Hz 请求值，窗口能力由 Android 适配层处理。 */
fun targetPlaybackRefreshRate(enabled: Boolean, idleMs: Long, playing: Boolean, fullscreen: Boolean): Float = when {
    !enabled || idleMs < 1500L -> 0f
    playing || fullscreen -> 60f
    else -> 1f
}
