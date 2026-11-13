package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.application.BackgroundPlaybackPort
import com.aritxonly.bdfun.infra.playback.PlaybackForegroundService

/** 系统组件只装配进程依赖，通知与媒体会话实现在 infra:playback。 */
class BackgroundPlaybackService : PlaybackForegroundService() {
    override val playback: BackgroundPlaybackPort get() = AppGraph.backgroundPlayback
    override fun onCreate() { AppGraph.ensure(applicationContext); super.onCreate() }
    companion object {
        const val ACTION_START = PlaybackForegroundService.ACTION_START
        const val ACTION_TOGGLE = PlaybackForegroundService.ACTION_TOGGLE
        const val ACTION_PREV = PlaybackForegroundService.ACTION_PREV
        const val ACTION_NEXT = PlaybackForegroundService.ACTION_NEXT
        const val ACTION_STOP = PlaybackForegroundService.ACTION_STOP
    }
}
