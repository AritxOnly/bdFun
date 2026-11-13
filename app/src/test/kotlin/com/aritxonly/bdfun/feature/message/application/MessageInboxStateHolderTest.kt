package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.message.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MessageInboxStateHolderTest {
    private class Fake : MessageSessionsRepository {
        var page = MessageInboxPage(emptyList(), null)
        var requested: Pair<String?, Int>? = null
        var cleared = 0
        var gate: CompletableDeferred<Unit>? = null
        var result: Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun sessions(cursor: Long, onEnriched: (List<MessageSession>) -> Unit): Outcome<MessageSessionPage> = error("新收件箱不能回退 HTTP")
        override suspend fun inbox(cursor: String?, pageType: Int, onEnriched: (List<MessageSession>) -> Unit): Outcome<MessageInboxPage> {
            requested = cursor to pageType; return Outcome.Success(page)
        }
        override suspend fun unread() = Outcome.Success(mapOf("at" to 2))
        override suspend fun clearUnread(sessions: List<MessageSession>): Outcome<Boolean> = error("不能枚举旧列表来假装 ClearUnread")
        override suspend fun clearInbox(pageType: Int): Outcome<Unit> { cleared = pageType; gate?.await(); return result }
        override suspend fun readInbox(pageType: Int, sessions: List<MessageSession>): Outcome<Boolean> { cleared = pageType; return Outcome.Success(true) }
        override suspend fun mutate(session: MessageSession, action: SessionAction) = Outcome.Success(Unit)
    }
    private fun item() = MessageSession("private", 1, 1, "姓名", "", "", 0, 1, false)
    @Test fun opaqueCursorAndSecondaryPageSurvivePaginationWithoutLongConversion() = runBlocking {
        val source = Fake().apply { page = MessageInboxPage(listOf(item()), "opaque-offset", listOf(MessageMenuItem(9, "清空列表"))) }
        val holder = MessageSessionsStateHolder(source, pageType = 3)
        holder.load(); source.page = MessageInboxPage(emptyList(), null); holder.loadFooter()
        assertEquals("opaque-offset" to 3, source.requested)
        assertEquals(9, holder.state.value.menu.single().type)
        assertEquals(1, holder.state.value.sessions.size)
    }
    @Test fun failedClearPreservesRowsAndCursorAndUnlocks() = runBlocking {
        val source = Fake().apply { page = MessageInboxPage(listOf(item()), "offset"); result = Outcome.Failure(AppError.Network("offline")) }
        val holder = MessageSessionsStateHolder(source, pageType = 4); holder.load()
        assertEquals("offline", holder.clearList())
        assertEquals(listOf(item()), holder.state.value.sessions); assertEquals("offset", holder.state.value.inboxCursor)
        assertEquals(4, source.cleared); assertFalse(holder.state.value.busy)
    }
    @Test fun successfulClearOnlyClearsCurrentInbox() = runBlocking {
        val source = Fake().apply { page = MessageInboxPage(listOf(item()), "offset") }
        val holder = MessageSessionsStateHolder(source, pageType = 2); holder.load(); holder.clearList()
        assertEquals(2, source.cleared); assertTrue(holder.state.value.sessions.isEmpty()); assertNull(holder.state.value.inboxCursor)
    }
    @Test fun cancelledClearKeepsRowsAndUnlocks() = runBlocking {
        val source = Fake().apply { page = MessageInboxPage(listOf(item()), null); gate = CompletableDeferred() }
        val holder = MessageSessionsStateHolder(source); holder.load()
        val task = launch(start = CoroutineStart.UNDISPATCHED) { holder.clearList() }; task.cancelAndJoin()
        assertEquals(1, holder.state.value.sessions.size); assertFalse(holder.state.value.busy)
    }
}
