package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.infra.bili.message.*
import org.json.JSONObject
import com.aritxonly.bdfun.core.model.message.*

class BiliMessageSessionsRepository(private val backend: BiliBackend) : MessageSessionsRepository {
    override suspend fun sessions(cursor: Long, onEnriched: (List<MessageSession>) -> Unit) =
        biliOutcome { backend.messageSessions(false, cursor, onEnriched = onEnriched) }
    override suspend fun inbox(cursor: String?, pageType: Int, onEnriched: (List<MessageSession>) -> Unit) = biliOutcome {
        val request = JSONObject().put("pagination_params", cursor?.let(::JSONObject) ?: JSONObject())
        val data = if (pageType == 1) backend.api.imRequest("SessionMain", "SessionMainReq", "SessionMainReply", request)
            else backend.api.imRequest("SessionSecondary", "SessionSecondaryReq", "SessionSecondaryReply", request.put("page_type", pageType))
        mapImInbox(data, cursor)
    }
    override suspend fun clearInbox(pageType: Int) = biliOutcome {
        backend.api.imRequest("DeleteSessionList", "DeleteSessionListReq", "DeleteSessionListReply", JSONObject().put("page_type", pageType)); Unit
    }
    override suspend fun readInbox(pageType: Int, sessions: List<MessageSession>) = biliOutcome {
        backend.api.imRequest("ClearUnread", "ClearUnreadReq", "ClearUnreadReply", JSONObject().put("page_type", pageType)); true
    }
    override suspend fun unread() = biliOutcome {
        val data = backend.api.imRequest("GetTotalUnread", "ReqTotalUnread", "RspTotalUnread",
            JSONObject().put("unread_type", 2).put("show_unfollow_list", 1), legacy = true)
        data.optJSONObject("msg_feed_unread")?.optJSONArray("unread").imObjects()
            .associate { it.optString("key") to it.optInt("value") }
    }
    override suspend fun clearUnread(sessions: List<MessageSession>) = biliOutcome { backend.clearAllUnread(sessions) }
    override suspend fun mutate(session: MessageSession, action: SessionAction) = biliOutcome {
        when (action) {
            SessionAction.Delete -> backend.removeMessageSession(session)
            SessionAction.Pin -> {
                val method = if (session.pinned) "UnpinSession" else "PinSession"
                val type = if (session.pinned) "UnPinSession" else "PinSession"
                val identity = session.preferences.identity.ifBlank {
                    JSONObject().put("private_id", JSONObject().put("talker_uid", session.talkerId)).toString()
                }
                backend.api.imRequest(method, type + "Req", type + "Reply", JSONObject().put("session_id", JSONObject(identity)))
            }
            SessionAction.Mute -> backend.muteMessageSession(session, !session.muted)
            SessionAction.Read -> backend.markSessionsRead(listOf(session))
        }
        Unit
    }
}
