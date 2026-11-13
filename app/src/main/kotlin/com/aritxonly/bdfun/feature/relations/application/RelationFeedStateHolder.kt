package com.aritxonly.bdfun.feature.relations.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.RelationRepository
import com.aritxonly.bdfun.core.model.user.RelationUser
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RelationFeedState(val items: List<RelationUser> = emptyList(), val total: Int = 0,
    val loading: Boolean = false, val error: String? = null, val nextPage: Int? = 1,
    val busyUsers: Set<Long> = emptySet())

/** 失败保留已有行与原游标；账号或页面失效后不继续提交请求与状态。 */
internal class RelationFeedStateHolder(private val repository: RelationRepository,
    private val mid: Long, private val fans: Boolean, private val frequent: Boolean,
    private val owner: Long?, private val currentOwner: () -> Long?, private val isLoggedIn: () -> Boolean,
    private val isCurrentOwner: () -> Boolean = { true }) {
    private val mutable = MutableStateFlow(RelationFeedState())
    val state = mutable.asStateFlow()
    val items get() = state.value.items
    val total get() = state.value.total
    val loading get() = state.value.loading
    val error get() = state.value.error
    val nextPage get() = state.value.nextPage
    private var retryReset = false
    private var closed = false
    fun close() { closed = true }
    private fun current() = !closed && isCurrentOwner() && owner == currentOwner()
    suspend fun fetch(reset: Boolean = false) {
        if (!current() || loading) return
        val page = if (reset) 1 else nextPage ?: return
        mutable.update { it.copy(loading = true, error = null) }
        retryReset = reset
        try {
            val result = repository.users(mid, fans, page, frequent)
            currentCoroutineContext().ensureActive()
            if (!current()) return
            mutable.update { current -> when (result) {
                is Outcome.Success -> {
                    val previous = if (reset) emptyList() else current.items
                    val merged = (previous + result.value.items).distinctBy { it.mid }
                    current.copy(items = merged, total = result.value.total,
                        nextPage = result.value.nextPage.takeUnless { !reset && merged.size == previous.size })
                }
                is Outcome.Failure -> current.copy(error = result.error.relationMessage())
            } }
        } finally { mutable.update { it.copy(loading = false) } }
    }
    suspend fun retry() = fetch(retryReset)
    fun updateRelation(mid: Long, attribute: Int) {
        if (current()) mutable.update { state -> state.copy(items = state.items.map {
            if (it.mid == mid) it.copy(attribute = attribute) else it
        }) }
    }
    suspend fun changeFollow(user: RelationUser): String? {
        if (!current()) return null
        if (!isLoggedIn()) return "请先登录"
        if (user.mid in state.value.busyUsers) return null
        mutable.update { it.copy(busyUsers = it.busyUsers + user.mid) }
        try {
            if (user.attribute == null) {
                val result = repository.attribute(user.mid)
                currentCoroutineContext().ensureActive()
                if (!current()) return null
                return when (result) {
                    is Outcome.Success -> {
                        val attribute = result.value ?: return "关注状态获取失败，请重试"
                        updateRelation(user.mid, attribute); null
                    }
                    is Outcome.Failure -> result.error.relationMessage()
                }
            }
            val follow = !user.followed
            val result = repository.follow(user.mid, follow)
            currentCoroutineContext().ensureActive()
            if (!current()) return null
            return when (result) {
                is Outcome.Success -> {
                    updateRelation(user.mid, if (follow) 2 else 0)
                    if (follow) "关注成功" else "已取消关注"
                }
                is Outcome.Failure -> result.error.relationMessage()
            }
        } finally { mutable.update { it.copy(busyUsers = it.busyUsers - user.mid) } }
    }
}
private fun AppError.relationMessage(): String = when (this) {
    is AppError.Network -> message
    is AppError.Unknown -> message
    is AppError.Parsing -> message
    is AppError.RiskControl -> message
    AppError.LoginExpired -> "请先登录"
}
