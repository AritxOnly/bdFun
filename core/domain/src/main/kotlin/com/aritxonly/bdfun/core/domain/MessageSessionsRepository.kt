package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.message.*

interface MessageSessionsRepository {
    suspend fun sessions(cursor: Long, onEnriched: (List<MessageSession>) -> Unit): Outcome<MessageSessionPage>
    suspend fun unread(): Outcome<Map<String, Int>>
    suspend fun clearUnread(sessions: List<MessageSession>): Outcome<Boolean>
    suspend fun mutate(session: MessageSession, action: SessionAction): Outcome<Unit>
}

enum class SessionAction { Delete, Pin, Mute, Read }
