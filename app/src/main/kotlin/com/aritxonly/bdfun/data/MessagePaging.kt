package com.aritxonly.bdfun.data

/** A filtered/empty visible page must still advance by the server cursor. */
internal fun messageContinuation(previous: MessageCursor?, id: Long, time: Long, ended: Boolean): MessageCursor? =
    MessageCursor(id, time).takeIf { !ended && id > 0L && it != previous }

internal fun sessionContinuation(previous: Long, next: Long, hasMore: Boolean): Long? =
    next.takeIf { hasMore && it > 0L && (previous == 0L || it < previous) }

internal fun messageFeedParams(type: Int, cursor: MessageCursor?): Map<String, String> {
    require(type in 0..2)
    return buildMap {
        put("platform", "web"); put("mobi_app", "web"); put("build", "0")
        put("web_location", "333.40164")
        cursor?.let {
            put("id", it.id.toString())
            put(listOf("reply_time", "at_time", "like_time")[type], it.time.toString())
        }
    }
}

internal fun messageDeleteType(type: Int): Int = when (type) {
    0 -> 1 // Reply
    1 -> 2 // Mention
    2 -> 0 // Like
    else -> error("Unknown message feed")
}

internal fun sessionPinOperation(pinned: Boolean): String = if (pinned) "0" else "1"

/** Complete every acknowledgement or propagate the failure; never turn partial success into true. */
internal suspend fun acknowledgeUnreadSessions(sessions: List<MessageSession>, ack: suspend (MessageSession) -> Unit) {
    for (session in sessions.groupBy { it.id }.values.map { group -> group.maxBy { it.maxSeqno } }.filter { it.unread > 0 }) {
        check(session.maxSeqno > 0L) { "会话缺少已读位置，请刷新后重试" }
        ack(session)
    }
}
