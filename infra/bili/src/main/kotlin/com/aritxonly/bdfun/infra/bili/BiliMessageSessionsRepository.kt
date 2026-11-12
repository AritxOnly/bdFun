package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.message.*

class BiliMessageSessionsRepository(private val backend: BiliBackend) : MessageSessionsRepository {
    override suspend fun sessions(cursor: Long, onEnriched: (List<MessageSession>) -> Unit) =
        biliOutcome { backend.messageSessions(false, cursor, onEnriched = onEnriched) }
    override suspend fun unread() = biliOutcome { backend.messageFeedUnread() }
    override suspend fun clearUnread(sessions: List<MessageSession>) = biliOutcome { backend.clearAllUnread(sessions) }
    override suspend fun mutate(session: MessageSession, action: SessionAction) = biliOutcome {
        when (action) {
            SessionAction.Delete -> backend.removeMessageSession(session)
            SessionAction.Pin -> backend.pinMessageSession(session, !session.pinned)
            SessionAction.Mute -> backend.muteMessageSession(session, !session.muted)
            SessionAction.Read -> backend.markSessionsRead(listOf(session))
        }
        Unit
    }
}
