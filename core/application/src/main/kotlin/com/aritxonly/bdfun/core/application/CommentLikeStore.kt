package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CommentLike(val liked: Boolean, val count: Int)

/** 账号内共享的有限点赞快照；令牌阻止账号切换前的写操作恢复旧账号状态。 */
class CommentLikeStore(private val capacity: Int = 300) {
    init { require(capacity > 0) }
    private val mutableState = MutableStateFlow<Map<Long, CommentLike>>(emptyMap())
    val state = mutableState.asStateFlow()
    private val mutableGeneration = MutableStateFlow(0L)
    val generationState = mutableGeneration.asStateFlow()
    val generation: Long get() = mutableGeneration.value

    fun put(rpid: Long, liked: Boolean, count: Int, token: Long = generation) {
        if (token != generation) return
        val next = LinkedHashMap(mutableState.value)
        next[rpid] = CommentLike(liked, count)
        while (next.size > capacity) next.remove(next.keys.first())
        mutableState.value = next
    }

    fun clear() { mutableGeneration.value++; mutableState.value = emptyMap() }
}
