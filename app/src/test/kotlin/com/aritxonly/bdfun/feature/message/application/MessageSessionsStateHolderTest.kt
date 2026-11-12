package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.message.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MessageSessionsStateHolderTest {
    private fun item(id: String = "one") = MessageSession(id, 1, 1, "旧姓名", "", "消息", 1, 5, false)
    private class Fake : MessageSessionsRepository {
        var page = MessageSessionPage(emptyList(), null)
        var result: Outcome<Unit> = Outcome.Success(Unit)
        var actionGate: CompletableDeferred<Unit>? = null
        var clearCalls = 0
        var sessionCalls = 0
        var callback: ((List<MessageSession>) -> Unit)? = null
        var cursor = -1L
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun sessions(cursor: Long, onEnriched: (List<MessageSession>) -> Unit): Outcome<MessageSessionPage> {
            sessionCalls++
            this.cursor = cursor; callback = onEnriched; gate?.await()
            return Outcome.Success(page)
        }
        override suspend fun unread() = Outcome.Success(mapOf("reply" to 3))
        override suspend fun clearUnread(sessions: List<MessageSession>): Outcome<Boolean> { clearCalls++; actionGate?.await(); return Outcome.Success(true) }
        override suspend fun mutate(session: MessageSession, action: SessionAction): Outcome<Unit> { actionGate?.await(); return result }
    }

    @Test fun paginationDeduplicatesAndPreservesPinnedOrder() = runBlocking {
        val source = Fake(); val holder = MessageSessionsStateHolder(source)
        source.page = MessageSessionPage(listOf(item()), 42)
        holder.load()
        source.page = MessageSessionPage(listOf(item(), item("two").let { it.copy(
            preferences = it.preferences.copy(
                pinned = true,
            ),
        ) }), null)
        holder.load(true)
        assertEquals(42L, source.cursor)
        assertEquals(listOf("two", "one"), holder.state.value.sessions.map { it.id })
        assertEquals(3, holder.state.value.unread["reply"])
        assertNull(holder.state.value.next)
    }

    @Test fun staleNameEnrichmentCannotUndoMutation() = runBlocking {
        val source = Fake(); source.page = MessageSessionPage(listOf(item()), null)
        val holder = MessageSessionsStateHolder(source); holder.load()
        val callback = source.callback!!
        holder.mutate(item(), SessionAction.Pin)
        callback(listOf(item().copy(name = "过期姓名")))
        assertEquals("旧姓名", holder.state.value.sessions.single().name)
        assertTrue(holder.state.value.sessions.single().pinned)
    }

    @Test fun failedMutationKeepsOriginalAndReleasesBusy() = runBlocking {
        val source = Fake(); source.page = MessageSessionPage(listOf(item()), null)
        val holder = MessageSessionsStateHolder(source); holder.load()
        source.result = Outcome.Failure(AppError.Network("offline"))
        assertTrue(holder.mutate(item(), SessionAction.Delete) is Outcome.Failure)
        assertEquals(listOf(item()), holder.state.value.sessions)
        assertFalse(holder.state.value.busy)
    }

    @Test fun logoutInvalidatesInFlightLoad() = runBlocking {
        val source = Fake(); source.page = MessageSessionPage(listOf(item()), null)
        source.gate = CompletableDeferred()
        val holder = MessageSessionsStateHolder(source)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        holder.reset(); source.gate!!.complete(Unit); job.join()
        assertTrue(holder.state.value.sessions.isEmpty())
        assertTrue(holder.state.value.unread.isEmpty())
        assertFalse(holder.state.value.loading)
    }

    @Test fun cancellationClearsLoadingAndDoesNotBecomeError() = runBlocking {
        val source = Fake(); source.gate = CompletableDeferred()
        val holder = MessageSessionsStateHolder(source)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        job.cancelAndJoin()
        assertFalse(holder.state.value.loading)
        assertNull(holder.state.value.error)
    }

    @Test fun closeRejectsLatePageAndEnrichmentAndFutureLoads() = runBlocking {
        val source = Fake(); source.page = MessageSessionPage(listOf(item()), 42)
        source.gate = CompletableDeferred()
        val holder = MessageSessionsStateHolder(source)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        holder.close()
        source.callback!!(listOf(item().copy(name = "旧账号")))
        source.gate!!.complete(Unit); job.join(); holder.load()
        assertEquals(1, source.sessionCalls)
        assertTrue(holder.state.value.sessions.isEmpty())
        assertTrue(holder.state.value.unread.isEmpty())
        assertFalse(holder.state.value.loading)
    }

    @Test fun closeRejectsLateMutationAndReadAllRefresh() = runBlocking {
        for (clear in listOf(false, true)) {
            val source = Fake(); source.page = MessageSessionPage(listOf(item()), null)
            val holder = MessageSessionsStateHolder(source); holder.load()
            source.actionGate = CompletableDeferred()
            val job = async(start = CoroutineStart.UNDISPATCHED) {
                if (clear) holder.clearUnread() else holder.mutate(item(), SessionAction.Pin)
            }
            holder.close(); source.actionGate!!.complete(Unit)
            assertNull(job.await()); assertEquals(1, source.sessionCalls)
            assertTrue(holder.state.value.sessions.isEmpty()); assertFalse(holder.state.value.busy)
        }
    }

    @Test fun footerReadsCurrentCursorAfterEachPage() = runBlocking {
        val source = Fake(); val holder = MessageSessionsStateHolder(source)
        source.page = MessageSessionPage(listOf(item()), 42); holder.loadFooter()
        assertEquals(0L, source.cursor)
        source.page = MessageSessionPage(listOf(item("two")), 84); holder.loadFooter()
        assertEquals(42L, source.cursor)
        source.page = MessageSessionPage(listOf(item("three")), null); holder.loadFooter()
        assertEquals(84L, source.cursor)
        assertEquals(3, holder.state.value.sessions.size)
    }

    @Test fun closeDuringReadAllRefreshSuppressesCompletionToast() = runBlocking {
        val source = Fake(); source.page = MessageSessionPage(listOf(item()), null)
        val holder = MessageSessionsStateHolder(source); holder.load()
        source.gate = CompletableDeferred()
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.clearUnread() }
        assertEquals(2, source.sessionCalls)
        holder.close(); source.gate!!.complete(Unit)
        assertNull(job.await())
        assertTrue(holder.state.value.sessions.isEmpty())
        assertFalse(holder.state.value.loading)
    }
}
