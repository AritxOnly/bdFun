package com.aritxonly.bdfun.feature.search.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.SearchFilterState
import com.aritxonly.bdfun.core.domain.SearchRepository
import com.aritxonly.bdfun.core.domain.SearchRequest
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SearchUiState(
    val hotKeywords: List<String> = emptyList(), val results: List<VideoItem> = emptyList(),
    val users: List<SearchUser> = emptyList(), val suggestions: List<String> = emptyList(),
    val loading: Boolean = false, val loadedOnce: Boolean = false, val error: String? = null,
)

internal class SearchStateHolder(
    private val repository: SearchRepository,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow(SearchUiState())
    val state = mutable.asStateFlow()
    private var searchRevision = 0
    private var suggestionRevision = 0
    private var lastUserKeyword: String? = null

    suspend fun loadHotKeywords(enabled: Boolean) {
        if (!enabled || mutable.value.hotKeywords.isNotEmpty()) return
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
        if (revision != suggestionRevision) return
        val result = repository.suggestions(word)
        if (revision != suggestionRevision) return
        mutable.update { it.copy(suggestions = when (result) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> emptyList()
        }) }
    }

    suspend fun search(submitted: String?, filters: SearchFilterState) {
        val revision = ++searchRevision
        if (submitted == null) {
            lastUserKeyword = null
            mutable.update { it.copy(results = emptyList(), users = emptyList(), error = null,
                loadedOnce = false, loading = false) }
            return
        }
        mutable.update { it.copy(loading = true, loadedOnce = false, error = null) }
        try {
            wait(if (lastUserKeyword != submitted) 90 else 180)
            if (revision != searchRevision) return
            if (lastUserKeyword != submitted) {
                when (val result = repository.users(submitted)) {
                    is Outcome.Success -> if (revision == searchRevision) {
                        lastUserKeyword = submitted
                        mutable.update { it.copy(users = result.value) }
                    }
                    is Outcome.Failure -> Unit
                }
            }
            if (revision != searchRevision) return
            val result = repository.videos(SearchRequest(
                submitted, filters.orderParam, filters.durationParam, filters.beginSecondsAt(nowMillis),
                if (filters.beginSecondsAt(nowMillis) != null) nowMillis() / 1000L else null,
            ))
            if (revision != searchRevision) return
            mutable.update { current -> when (result) {
                is Outcome.Success -> current.copy(results = result.value, loadedOnce = true)
                is Outcome.Failure -> current.copy(error = when (val error = result.error) {
                    is AppError.Unknown -> error.message
                    else -> error.messageOr("搜索失败")
                }, loadedOnce = true)
            } }
        } finally {
            if (revision == searchRevision) mutable.update { it.copy(loading = false) }
        }
    }
}
