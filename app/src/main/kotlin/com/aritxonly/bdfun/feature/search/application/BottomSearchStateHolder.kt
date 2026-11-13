package com.aritxonly.bdfun.feature.search.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.SearchRepository
import com.aritxonly.bdfun.core.domain.SearchRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 两个搜索入口保留各自请求时序；底部搜索不引入顶栏的筛选和用户缓存。 */
internal class BottomSearchStateHolder(
    private val repository: SearchRepository,
    private val wait: suspend (Long) -> Unit = { delay(it) },
) {
    private val mutable = MutableStateFlow(SearchUiState())
    val state = mutable.asStateFlow()
    private var searchRevision = 0
    private var suggestionRevision = 0

    suspend fun loadHotKeywords(enabled: Boolean) {
        if (!enabled) return
        when (val result = repository.hotKeywords()) {
            is Outcome.Success -> mutable.update { it.copy(hotKeywords = result.value) }
            is Outcome.Failure -> Unit
        }
    }

    suspend fun suggest(query: String, submitted: String?) {
        val revision = ++suggestionRevision
        val word = query.trim()
        if (submitted != null || word.isBlank()) {
            mutable.update { it.copy(suggestions = emptyList()) }
            return
        }
        wait(180)
        val result = repository.suggestions(word)
        if (revision != suggestionRevision) return
        mutable.update { it.copy(suggestions = when (result) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> emptyList()
        }) }
    }

    suspend fun search(submitted: String?) {
        val revision = ++searchRevision
        if (submitted == null) {
            mutable.update { it.copy(results = emptyList(), users = emptyList(), error = null,
                loadedOnce = false, loading = false) }
            return
        }
        mutable.update { it.copy(loading = true, loadedOnce = false, error = null) }
        try {
            wait(90)
            if (revision != searchRevision) return
            when (val users = repository.users(submitted)) {
                is Outcome.Success -> if (revision == searchRevision) mutable.update { it.copy(users = users.value) }
                is Outcome.Failure -> Unit
            }
            if (revision != searchRevision) return
            val result = repository.videos(SearchRequest(submitted, "totalrank", 0, null, null))
            if (revision != searchRevision) return
            mutable.update { current -> when (result) {
                is Outcome.Success -> current.copy(results = result.value, loadedOnce = true)
                is Outcome.Failure -> current.copy(error = result.error.messageOr("搜索失败"), loadedOnce = true)
            } }
        } finally {
            if (revision == searchRevision) mutable.update { it.copy(loading = false) }
        }
    }
}
