package com.aritxonly.bdfun.data

internal fun messageContinuation(previous: MessageCursor?, id: Long, time: Long, ended: Boolean) =
    com.aritxonly.bdfun.core.domain.messageContinuation(previous, id, time, ended)

internal fun sessionContinuation(previous: Long, next: Long, hasMore: Boolean) =
    com.aritxonly.bdfun.core.domain.sessionContinuation(previous, next, hasMore)

internal fun messageFeedParams(type: Int, cursor: MessageCursor?) =
    com.aritxonly.bdfun.core.domain.messageFeedParams(type, cursor)

internal fun messageDeleteType(type: Int) =
    com.aritxonly.bdfun.core.domain.messageDeleteType(type)

internal fun sessionPinOperation(pinned: Boolean) =
    com.aritxonly.bdfun.core.domain.sessionPinOperation(pinned)

internal suspend fun acknowledgeUnreadSessions(sessions: List<MessageSession>, ack: suspend (MessageSession) -> Unit) =
    com.aritxonly.bdfun.core.domain.acknowledgeUnreadSessions(sessions, ack)
