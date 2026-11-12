package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.flow.StateFlow

/** 唱片只观察当前会话和转发控制，不拥有原生播放器。 */
interface BackgroundDiscPlayback : BackgroundPlaybackPort {
    val state: StateFlow<PlaybackSessionState>
    val video: VideoItem?
    fun boostStart()
    fun boostEnd()
}
