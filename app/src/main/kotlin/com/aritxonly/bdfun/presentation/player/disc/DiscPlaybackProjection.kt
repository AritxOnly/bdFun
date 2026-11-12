package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.PlaybackSessionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal data class DiscPlaybackState(val cover: String, val playing: Boolean)

/** 进度和缓冲属于播放会话；唱片与原来的两个独立 Compose 字段一样只观察这些显示值。 */
internal fun observeDiscPlayback(source: StateFlow<PlaybackSessionState>) = source.map {
    DiscPlaybackState(it.cover, it.playback.playing)
}.distinctUntilChanged()
