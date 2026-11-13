package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.MessageBadgeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MessageBadgeStateHolderTest {
    @Test fun guestStartsAtZeroAndDoesNotRequest() = runBlocking {
        val holder = MessageBadgeStateHolder(MessageBadgeRepository { error("游客不请求") }, null)
        holder.refresh()
        assertEquals(0, holder.state.value.unreadCount)
    }

    @Test fun successReplacesCountIncludingZero() = runBlocking {
        var count = 12
        val holder = MessageBadgeStateHolder(MessageBadgeRepository { Outcome.Success(count) }, 1)
        holder.refresh()
        assertEquals(12, holder.state.value.unreadCount)
        count = 0
        holder.refresh()
        assertEquals(0, holder.state.value.unreadCount)
    }

    @Test fun failedRefreshKeepsLastSuccessfulCount() = runBlocking {
        var response: Outcome<Int> = Outcome.Success(8)
        val holder = MessageBadgeStateHolder(MessageBadgeRepository { response }, 1)
        holder.refresh()
        response = Outcome.Failure(AppError.Network("offline"))
        holder.refresh()
        assertEquals(8, holder.state.value.unreadCount)
    }

    @Test fun leavingForegroundCancelsRequestWithoutClearingCount() = runBlocking {
        var gate: CompletableDeferred<Outcome<Int>>? = null
        val holder = MessageBadgeStateHolder(MessageBadgeRepository {
            gate?.await() ?: Outcome.Success(4)
        }, 1)
        holder.refresh()
        gate = CompletableDeferred()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.refresh() }
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertEquals(4, holder.state.value.unreadCount)
    }

    @Test fun previousAccountResponseCannotChangeNewAccountBadge() = runBlocking {
        val oldResponse = CompletableDeferred<Outcome<Int>>()
        val old = MessageBadgeStateHolder(MessageBadgeRepository { oldResponse.await() }, 1)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { old.refresh() }
        val current = MessageBadgeStateHolder(MessageBadgeRepository { Outcome.Success(3) }, 2)
        assertEquals(0, current.state.value.unreadCount)
        current.refresh()
        oldResponse.complete(Outcome.Success(99))
        job.join()
        assertEquals(3, current.state.value.unreadCount)
    }
}
