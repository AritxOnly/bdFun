package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.MessageBadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class MessageBadgeUiState(val unreadCount: Int = 0)

/** 由主界面按账号创建；离开前台只停轮询，不清掉上次成功显示的数值。 */
internal class MessageBadgeStateHolder(
    private val repository: MessageBadgeRepository,
    private val owner: Long?,
) {
    private val mutable = MutableStateFlow(MessageBadgeUiState())
    val state = mutable.asStateFlow()

    suspend fun refresh() {
        if (owner == null) return
        when (val result = repository.unreadCount()) {
            is Outcome.Success -> mutable.value = MessageBadgeUiState(result.value)
            is Outcome.Failure -> Unit // 失败保留原计数，不干扰首页。
        }
    }
}
