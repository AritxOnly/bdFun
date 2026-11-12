package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackQueueState(val items: List<VideoItem> = emptyList(), val index: Int = -1)

/** 收藏/历史/稍后再看的队列由进程容器持有，顺序与旧连播规则一致。 */
class PlaybackQueueStateHolder {
    private val mutableState = MutableStateFlow(PlaybackQueueState())
    val state = mutableState.asStateFlow()
    fun set(list: List<VideoItem>, bvid: String) {
        val snapshot = list.toList()
        mutableState.value = PlaybackQueueState(snapshot, snapshot.indexOfFirst { it.bvid == bvid })
    }
    fun contains(bvid: String): Boolean = state.value.items.any { it.bvid == bvid }
    fun hasNext(): Boolean = state.value.let { it.index >= 0 && it.index + 1 < it.items.size }
    fun next(): VideoItem? {
        val before = state.value
        if (before.index < 0 || before.index + 1 >= before.items.size) return null
        val next = before.copy(index = before.index + 1)
        mutableState.value = next
        return next.items[next.index]
    }
    fun clear() { mutableState.value = PlaybackQueueState() }
}
