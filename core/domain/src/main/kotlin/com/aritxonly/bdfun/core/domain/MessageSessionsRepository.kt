package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.message.*

interface MessageSessionsRepository {
    suspend fun sessions(cursor: Long, onEnriched: (List<MessageSession>) -> Unit): Outcome<MessageSessionPage>
    suspend fun inbox(cursor: String?, pageType: Int = 1, onEnriched: (List<MessageSession>) -> Unit = {}): Outcome<MessageInboxPage> =
        when (val result = sessions(cursor?.toLongOrNull() ?: 0L, onEnriched)) {
            is Outcome.Success -> Outcome.Success(MessageInboxPage(result.value.items, result.value.next?.toString()))
            is Outcome.Failure -> result
        }
    suspend fun clearInbox(pageType: Int): Outcome<Unit> = com.aritxonly.bdfun.core.common.Outcome.Failure(
        com.aritxonly.bdfun.core.common.AppError.Unknown("服务未提供清空列表"))
    suspend fun readInbox(pageType: Int, sessions: List<MessageSession>): Outcome<Boolean> = clearUnread(sessions)
    suspend fun unread(): Outcome<Map<String, Int>>
    suspend fun clearUnread(sessions: List<MessageSession>): Outcome<Boolean>
    suspend fun mutate(session: MessageSession, action: SessionAction): Outcome<Unit>
}

enum class SessionAction { Delete, Pin, Mute, Read }
