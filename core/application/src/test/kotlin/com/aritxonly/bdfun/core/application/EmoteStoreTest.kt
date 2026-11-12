package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.EmoteRepository
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class EmoteStoreTest {
    private class Fake : EmoteRepository {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        var result: Outcome<List<EmoteItem>> = Outcome.Success(listOf(EmoteItem("[doge]", "url")))
        override suspend fun replyEmotes(): Outcome<List<EmoteItem>> { calls++; gate?.await(); return result }
    }
    @Test fun concurrentPreloadDoesNotDuplicateRequestAndBuildsLookup() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }
        val store = EmoteStore(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { store.ensure() }
        store.ensure(); assertEquals(1, fake.calls)
        fake.gate!!.complete(Unit); job.join()
        assertEquals("url", store.state.value.byText["[doge]"])
        store.ensure(); assertEquals(1, fake.calls)
    }
    @Test fun cancellationUnlocksPreloadWithoutNetworkError() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }
        val store = EmoteStore(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { store.ensure() }
        job.cancelAndJoin()
        assertFalse(store.state.value.loading)
        assertNull(store.state.value.error)
        fake.gate = null; store.ensure()
        assertFalse(store.state.value.items.isEmpty())
    }
    @Test fun accountChangeRejectsOldPackResponse() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }
        val store = EmoteStore(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { store.ensure() }
        store.clear(); fake.gate!!.complete(Unit); job.join()
        assertTrue(store.state.value.items.isEmpty())
        assertFalse(store.state.value.loading)
    }
    @Test fun failedPreloadCanBeRetried() = runBlocking {
        val fake = Fake().apply { result = Outcome.Failure(AppError.Network("offline")) }
        val store = EmoteStore(fake); store.ensure()
        assertFalse(store.state.value.loading)
        fake.result = Outcome.Success(listOf(EmoteItem("[doge]", "url")))
        store.ensure()
        assertEquals("url", store.state.value.byText["[doge]"])
    }
}
