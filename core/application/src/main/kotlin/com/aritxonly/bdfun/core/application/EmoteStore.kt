package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.EmoteRepository
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EmoteState(
    val items: List<EmoteItem> = emptyList(), val byText: Map<String, String> = emptyMap(),
    val loading: Boolean = false, val error: AppError? = null,
)

/** 跨评论/动态/私信共用；账号变化使旧请求失效，取消后可再次预加载。 */
class EmoteStore(private val repository: EmoteRepository) {
    private val mutableState = MutableStateFlow(EmoteState())
    val state = mutableState.asStateFlow()
    private var revision = 0L
    fun clear() { revision++; mutableState.value = EmoteState() }
    suspend fun ensure() {
        if (state.value.items.isNotEmpty() || state.value.loading) return
        val version = revision
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            val result = repository.replyEmotes()
            if (version != revision) return
            when (result) {
                is Outcome.Success -> {
                    val items = result.value.toList()
                    mutableState.value = EmoteState(items, items.filter { it.type != 4 && it.url.isNotBlank() }.associate { it.text to it.url })
                }
                is Outcome.Failure -> mutableState.update { it.copy(error = result.error) }
            }
        } finally { if (version == revision) mutableState.update { it.copy(loading = false) } }
    }
}
