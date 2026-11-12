package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.MessageFeedRepository
import com.aritxonly.bdfun.core.model.message.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MessageFeedStateHolderTest {
    private fun item(id: String = "one") = MessageItem(id, "标题", "正文", 1, "姓名", "", userMid = 1)
    private class Fake : MessageFeedRepository {
        var items = emptyList<MessageItem>()
        var next: MessageCursor? = null
        var type = -1
        var pageGate: CompletableDeferred<Unit>? = null
        var mutationGate: CompletableDeferred<Unit>? = null
        var pageCalls = 0
        var cursor: MessageCursor? = null
        var fanPage = 0
        var followResult: Outcome<Boolean> = Outcome.Success(true)
        var gate: CompletableDeferred<Unit>? = null
        var mutation: Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun page(type: Int, cursor: MessageCursor?): Outcome<MessageFeedPage> {
            pageCalls++; this.cursor = cursor; pageGate?.await()
            this.type = type; return Outcome.Success(MessageFeedPage(items, next))
        }
        override suspend fun followers(page: Int): Outcome<List<MessageItem>> { fanPage = page; return Outcome.Success(items) }
        override suspend fun follow(mid: Long, follow: Boolean): Outcome<Boolean> { gate?.await(); return followResult }
        override suspend fun dynamic(id: String) = Outcome.Success(null)
        override suspend fun delete(type: Int?, id: String): Outcome<Unit> { mutationGate?.await(); return mutation }
        override suspend fun notice(id: String, enabled: Boolean): Outcome<Unit> { mutationGate?.await(); return mutation }
    }

    @Test fun followingTabReusesAllFeedAndAtTabUsesSeparateEndpoint() = runBlocking {
        val source = Fake(); source.items = listOf(item())
        val holder = MessageFeedStateHolder(source, MessageFeedKind.Reply)
        holder.loadPage(1)
        assertTrue(holder.state.value.itemsByTab.containsKey(0)); assertEquals(0, source.type)
        holder.loadPage(2)
        assertTrue(holder.state.value.itemsByTab.containsKey(2)); assertEquals(1, source.type)
    }

    @Test fun fansAdvanceOnlyAfterSuccessfulFullPage() = runBlocking {
        val source = Fake(); source.items = List(20) { item(it.toString()) }
        val holder = MessageFeedStateHolder(source, MessageFeedKind.Fans)
        holder.loadPage(0); holder.loadPage(0, true)
        assertEquals(2, source.fanPage)
        assertEquals(20, holder.state.value.itemsByTab[0]!!.size)
    }

    @Test fun cancelledFollowRollsBackAndUnlocks() = runBlocking {
        val source = Fake(); source.gate = CompletableDeferred()
        val holder = MessageFeedStateHolder(source, MessageFeedKind.Fans)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.toggleFollow(item()) }
        assertTrue(holder.state.value.followedOverride[1] == true)
        assertNull(holder.toggleFollow(item()))
        job.cancelAndJoin()
        assertFalse(holder.state.value.followedOverride[1] == true)
        assertTrue(holder.state.value.followBusy.isEmpty())
    }

    @Test fun failedDeleteDoesNotRemoveNotification() = runBlocking {
        val source = Fake(); source.items = listOf(item()); source.mutation = Outcome.Failure(AppError.Network("offline"))
        val holder = MessageFeedStateHolder(source, MessageFeedKind.System); holder.loadPage(0)
        assertTrue(holder.mutate(0, item(), true) is Outcome.Failure)
        assertEquals(source.items, holder.state.value.itemsByTab[0])
        assertFalse(holder.state.value.busy)
    }

    @Test fun noticeMutationPreservesOtherFields() = runBlocking {
        val source = Fake(); source.items = listOf(item())
        val holder = MessageFeedStateHolder(source, MessageFeedKind.Like); holder.loadPage(0)
        holder.mutate(0, item(), false)
        assertEquals(listOf(item().copy(noticeState = 1)), holder.state.value.itemsByTab[0])
    }

    @Test fun closeRejectsLatePageAndFurtherRequests() = runBlocking {
        val source = Fake(); source.items = listOf(item()); source.pageGate = CompletableDeferred()
        val holder = MessageFeedStateHolder(source, MessageFeedKind.Reply)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadPage(0) }
        holder.close(); source.pageGate!!.complete(Unit); job.join(); holder.loadPage(0)
        assertEquals(1, source.pageCalls)
        assertTrue(holder.state.value.itemsByTab.isEmpty())
        assertTrue(holder.state.value.loadingByTab.isEmpty())
    }

    @Test fun closeRejectsLateDeleteOrNotice() = runBlocking {
        for (delete in listOf(false, true)) {
            val source = Fake(); source.items = listOf(item())
            val holder = MessageFeedStateHolder(source, MessageFeedKind.Like); holder.loadPage(0)
            source.mutationGate = CompletableDeferred()
            val job = async(start = CoroutineStart.UNDISPATCHED) { holder.mutate(0, item(), delete) }
            holder.close(); source.mutationGate!!.complete(Unit)
            assertNull(job.await()); assertEquals(source.items, holder.state.value.itemsByTab[0])
            assertFalse(holder.state.value.busy)
        }
    }

    @Test fun closeSuppressesFollowCompletionAndReleasesBusy() = runBlocking {
        val source = Fake(); source.gate = CompletableDeferred()
        val holder = MessageFeedStateHolder(source, MessageFeedKind.Fans)
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.toggleFollow(item()) }
        holder.close(); val closed = holder.state.value
        source.gate!!.complete(Unit)
        assertNull(job.await()); assertEquals(closed, holder.state.value)
        assertTrue(holder.state.value.followBusy.isEmpty())
    }

    @Test fun footerReusesAllForFollowingAndReadsLatestCursor() = runBlocking {
        val source = Fake(); val holder = MessageFeedStateHolder(source, MessageFeedKind.Reply)
        source.items = listOf(item()); source.next = MessageCursor(42)
        holder.loadFooter(1); assertNull(source.cursor); assertEquals(0, source.type)
        source.items = listOf(item("two")); source.next = MessageCursor(84)
        holder.loadFooter(1); assertEquals(MessageCursor(42), source.cursor)
        source.items = listOf(item("three")); source.next = null
        holder.loadFooter(0); assertEquals(MessageCursor(84), source.cursor)
        assertEquals(3, holder.state.value.itemsByTab[0]!!.size)
        holder.loadFooter(2); assertEquals(1, source.type)
    }
}
