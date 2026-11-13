package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.EmoteRepository
import com.aritxonly.bdfun.core.domain.EmoteCache
import com.aritxonly.bdfun.core.domain.CachedEmotes
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EmoteState(
    val items: List<EmoteItem> = emptyList(), val byText: Map<String, String> = emptyMap(),
    val loading: Boolean = false, val error: AppError? = null,
)

/** 跨评论/动态/私信共用；账号变化使旧请求失效，取消后可再次预加载。 */
class EmoteStore(
    private val repository: EmoteRepository,
    private val cache: EmoteCache? = null,
    private val accountId: () -> Long = { 0L },
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutableState = MutableStateFlow(EmoteState())
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var freshnessTime: Long? = null
    fun clear() { revision++; freshnessTime = null; mutableState.value = EmoteState() }
    suspend fun ensure(force: Boolean = false) {
        if (state.value.loading) return
        val memoryAge = freshnessTime?.let { now() - it }
        if (!force && state.value.items.isNotEmpty() && memoryAge != null && memoryAge >= 0 && memoryAge < CACHE_TTL) return
        val version = revision
        val owner = accountId()
        fun valid() = version == revision && owner == accountId()
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            val saved = (cache?.read(owner) as? Outcome.Success)?.value
            if (!valid()) return
            if (saved != null && saved.items.isNotEmpty()) {
                freshnessTime = saved.savedAt
                publish(saved.items, loading = true)
                val age = now() - saved.savedAt
                if (!force && age >= 0 && age < CACHE_TTL) return
            }
            val result = repository.replyEmotes()
            if (!valid()) return
            when (result) {
                is Outcome.Success -> {
                    val items = result.value.toList()
                    // 空面板或暂时的鉴权失败不抹掉已经可用的本地列表。
                    if (items.isNotEmpty() || state.value.items.isEmpty()) {
                        freshnessTime = now()
                        publish(items, loading = true)
                    }
                    if (items.isNotEmpty()) cache?.write(owner, CachedEmotes(items, now()))
                }
                is Outcome.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        } finally { if (version == revision) mutableState.update { it.copy(loading = false) } }
    }
    private fun publish(items: List<EmoteItem>, loading: Boolean) {
        mutableState.value = EmoteState(items,
            items.filter { it.type != 4 && it.url.isNotBlank() }.associate { it.text to it.url }, loading)
    }
    companion object { private const val CACHE_TTL = 24 * 60 * 60 * 1000L }
}
