package com.aritxonly.bdfun.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class MessagePagingTest {
    @Test fun continuationUsesBothCursorFieldsAndNoPageNumber() {
        val cursor = MessageCursor(123, 456)
        for ((type, field) in listOf("reply_time", "at_time", "like_time").withIndex()) {
            val params = messageFeedParams(type, cursor)
            assertEquals("123", params["id"])
            assertEquals("456", params[field])
            assertFalse(params.containsKey("pn"))
            assertFalse(messageFeedParams(type, null).containsKey("id"))
        }
    }
    @Test fun serverEndAndUnchangedCursorStopButEmptyFilteredPagesCanContinue() {
        val cursor = MessageCursor(99, 500)
        assertNull(messageContinuation(cursor, 99, 500, false))
        assertNull(messageContinuation(cursor, 98, 499, true))
        assertNull(messageContinuation(cursor, 0, 0, false))
        assertEquals(MessageCursor(98, 499), messageContinuation(cursor, 98, 499, false))
        assertEquals(400L, sessionContinuation(500, 400, true))
        assertNull(sessionContinuation(500, 500, true))
        assertNull(sessionContinuation(500, 600, true))
        assertNull(sessionContinuation(500, 400, false))
    }
    @Test fun notificationDeleteAndPinUseServiceEnumsNotScreenIndexes() {
        assertEquals(1, messageDeleteType(0))
        assertEquals(2, messageDeleteType(1))
        assertEquals(0, messageDeleteType(2))
        assertEquals("0", sessionPinOperation(true))
        assertEquals("1", sessionPinOperation(false))
    }
    private fun session(id: String, unread: Int = 2, seq: Long = 10) =
        MessageSession(id, 1, 1, "", "", "", 0, unread, false, maxSeqno = seq)
    @Test fun partialAckFailureCannotReportAllReadOrMutateOriginalCounts() = runBlocking {
        val sessions = listOf(session("a"), session("b"))
        val calls = mutableListOf<String>()
        val result = runCatching { acknowledgeUnreadSessions(sessions) {
            calls += it.id
            if (it.id == "b") error("offline")
        } }
        assertTrue(result.isFailure)
        assertEquals(listOf("a", "b"), calls)
        assertEquals(listOf(2, 2), sessions.map { it.unread })
    }
    @Test fun readAckDeduplicatesAtNewestPositionAndIgnoresReadSessions() = runBlocking {
        val calls = mutableListOf<MessageSession>()
        acknowledgeUnreadSessions(listOf(session("a"), session("a", seq = 12), session("read", unread = 0))) { calls += it }
        assertEquals(1, calls.size)
        assertEquals(12L, calls.single().maxSeqno)
    }
    @Test fun missingPositionAndCancellationAreNotSuccess() = runBlocking {
        assertTrue(runCatching { acknowledgeUnreadSessions(listOf(session("a", seq = 0))) { fail() } }.isFailure)
        val result = runCatching { acknowledgeUnreadSessions(listOf(session("a"))) { throw CancellationException() } }
        assertTrue(result.exceptionOrNull() is CancellationException)
    }
}
