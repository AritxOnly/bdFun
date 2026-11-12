package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.application.videoTextTokens
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 缓存先填入；剩余标题依原出现顺序并行等待，全部结束后一起发布。 */
class LinkTitlesStateHolder(text: String, private val store: VideoLinkTitleStore) {
    private val tokens = videoTextTokens(text)
    private val mutable = MutableStateFlow<Map<String, ResolvedVideoLink>>(emptyMap())
    val state = mutable.asStateFlow()
    private val generation = store.generation.value
    private var closed = false
    private var loading = false
    fun close() { closed = true }
    suspend fun load() {
        if (closed || loading || generation != store.generation.value) return
        loading = true
        try {
            checkActive()
            val cached = tokens.mapNotNull { token -> store.cached(token)?.let { token to it } }.toMap()
            mutable.value = cached
            val results = coroutineScope {
                tokens.filterNot { cached.containsKey(it) }.map { token -> async { checkActive(); token to store.resolve(token) } }.awaitAll()
            }
            checkActive()
            mutable.value = cached + results.mapNotNull { (token, value) -> value?.let { token to it } }
        } finally { loading = false }
    }
    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        if (closed || generation != store.generation.value) throw CancellationException("正文已关闭")
    }
}
