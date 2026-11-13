package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.AccountRepository
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SessionState(
    val user: UserProfile? = null, val refreshing: Boolean = false,
    val error: AppError? = null, val revision: Long = 0,
)

/** 登录/退出使在途资料请求过期；瞬时网络失败不能抹掉已有登录资料。 */
class SessionStateHolder(private val repository: AccountRepository) : SessionStore {
    private val mutable = MutableStateFlow(SessionState())
    override val state = mutable.asStateFlow()
    override fun update(user: UserProfile?) {
        mutable.update { SessionState(user = user, revision = it.revision + 1) }
    }
    fun logout() { repository.logout(); update(null) }

    suspend fun refresh() {
        val initial = mutable.value
        if (initial.refreshing) return
        mutable.update { it.copy(refreshing = true, error = null) }
        try {
            val result = repository.profile()
            mutable.update { current ->
                if (current.revision != initial.revision) current else current.withProfile(result)
            }
        } finally { mutable.update { if (it.revision == initial.revision) it.copy(refreshing = false) else it } }
    }
    private fun SessionState.withProfile(result: Outcome<UserProfile?>): SessionState {
        val nextUser = when (result) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> if (result.error == AppError.LoginExpired) null else user
        }
        val changed = user?.mid != nextUser?.mid
        return copy(user = nextUser, error = (result as? Outcome.Failure)?.error,
            revision = revision + if (changed) 1 else 0,
            refreshing = if (changed) false else refreshing)
    }
}
