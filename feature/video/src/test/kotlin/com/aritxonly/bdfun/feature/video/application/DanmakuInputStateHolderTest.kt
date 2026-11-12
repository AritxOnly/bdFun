package com.aritxonly.bdfun.feature.video.application

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DanmakuInputStateHolderTest {
    @Test fun blankDraftDoesNotStartOrFailAndMissingSenderKeepsDraft() {
        val holder = DanmakuInputStateHolder(); holder.edit("  ")
        assertNull(holder.beginSend(null)); assertFalse(holder.state.value.failed)
        holder.edit("hello"); assertNull(holder.beginSend(null)); assertTrue(holder.state.value.failed)
        assertEquals("hello", holder.state.value.text); assertFalse(holder.state.value.sending)
    }
    @Test fun trimsAndUsesOriginalStyleThenClearsOnlyAfterSuccess() = runBlocking {
        val holder = DanmakuInputStateHolder(); holder.edit(" hi \n")
        val request = holder.beginSend { text, mode, size, color ->
            assertEquals("hi", text); assertEquals(1, mode); assertEquals(25, size); assertEquals(0xFFFFFF, color); true
        }!!
        assertTrue(holder.state.value.sending); assertEquals(" hi \n", holder.state.value.text)
        assertEquals(DanmakuInputResult.Success, holder.completeSend(request))
        assertEquals(DanmakuInputState(), holder.state.value)
    }
    @Test fun falseAndExceptionRetainDraftAndAllowRetry() = runBlocking {
        val holder = DanmakuInputStateHolder(); holder.edit("draft")
        assertEquals(DanmakuInputResult.Failure, holder.completeSend(holder.beginSend { _, _, _, _ -> false }!!))
        assertEquals("draft", holder.state.value.text); assertTrue(holder.state.value.failed)
        val retry = holder.beginSend { _, _, _, _ -> error("offline") }!!
        assertFalse(holder.state.value.failed); assertEquals(DanmakuInputResult.Failure, holder.completeSend(retry))
        assertFalse(holder.state.value.sending); assertEquals("draft", holder.state.value.text)
    }
    @Test fun repeatedBeginOrCompletionNeverDuplicatesSend() = runBlocking {
        val holder = DanmakuInputStateHolder(); holder.edit("draft"); var calls = 0
        val request = holder.beginSend { _, _, _, _ -> calls++; true }!!
        assertNull(holder.beginSend { _, _, _, _ -> error("duplicate") })
        holder.completeSend(request)
        assertEquals(DanmakuInputResult.Ignored, holder.completeSend(request)); assertEquals(1, calls)
    }
    @Test fun closeRejectsLateSuccessAndFutureEditsOrSends() = runBlocking {
        val holder = DanmakuInputStateHolder(); holder.edit("draft"); val gate = CompletableDeferred<Unit>()
        val request = holder.beginSend { _, _, _, _ -> gate.await(); true }!!
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.completeSend(request) }
        holder.close(); gate.complete(Unit); assertEquals(DanmakuInputResult.Ignored, job.await())
        holder.edit("late"); assertEquals("draft", holder.state.value.text); assertNull(holder.beginSend(null))
    }
    @Test fun cancellationIsNotFailureAndReleasesSendingForRetry() = runBlocking {
        val holder = DanmakuInputStateHolder(); holder.edit("draft"); val gate = CompletableDeferred<Unit>()
        val request = holder.beginSend { _, _, _, _ -> gate.await(); true }!!
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.completeSend(request) }; job.cancelAndJoin()
        assertFalse(holder.state.value.sending); assertFalse(holder.state.value.failed)
        assertNotNull(holder.beginSend { _, _, _, _ -> true })
    }
}
