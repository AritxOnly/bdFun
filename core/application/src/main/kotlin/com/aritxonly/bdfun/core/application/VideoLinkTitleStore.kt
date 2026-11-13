package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.VideoLinkTitleRepository
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 成功和失败都沿用进程缓存；共用一个有上限的 LRU，避免长时间浏览无限积累。 */
class VideoLinkTitleStore(private val repository: VideoLinkTitleRepository, private val capacity: Int = 4096) {
    init { require(capacity > 0) }
    private val lock = Any()
    private val values = LinkedHashMap<String, ResolvedVideoLink?>(16, 0.75f, true)
    private val mutableGeneration = MutableStateFlow(0L)
    val generation = mutableGeneration.asStateFlow()
    fun cached(token: String): ResolvedVideoLink? = synchronized(lock) { values[token] }
    fun clear() { synchronized(lock) { values.clear(); mutableGeneration.value++ } }
    suspend fun resolve(token: String): ResolvedVideoLink? {
        currentCoroutineContext().ensureActive()
        val version = synchronized(lock) {
            if (values.containsKey(token)) return values[token]
            mutableGeneration.value
        }
        val result = repository.resolve(token)
        currentCoroutineContext().ensureActive()
        val resolved = (result as? Outcome.Success)?.value
        synchronized(lock) {
            if (version != mutableGeneration.value) throw CancellationException("链接标题账号已失效")
            values[token] = resolved
            while (values.size > capacity) values.remove(values.keys.first())
        }
        return resolved
    }
}
