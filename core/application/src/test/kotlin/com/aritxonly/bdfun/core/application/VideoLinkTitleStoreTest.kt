package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.VideoLinkTitleRepository
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VideoLinkTitleStoreTest {
    private class Fake : VideoLinkTitleRepository {
        val calls = mutableListOf<String>()
        val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
        var failed = false
        override suspend fun resolve(token: String): Outcome<ResolvedVideoLink?> {
            calls += token
            gates[token]?.await()
            return if (failed) Outcome.Failure(AppError.Network("offline"))
            else Outcome.Success(ResolvedVideoLink(token, "title:$token"))
        }
    }
    @Test fun successAndFailureShareBoundedAccessOrderedCache() = runBlocking {
        val fake = Fake(); val store = VideoLinkTitleStore(fake, 2)
        store.resolve("a"); fake.failed = true; store.resolve("b")
        assertNull(store.resolve("b")); assertEquals(listOf("a", "b"), fake.calls)
        store.cached("a"); fake.failed = false; store.resolve("c")
        assertNull(store.cached("b")); store.resolve("b")
        assertEquals(listOf("a", "b", "c", "b"), fake.calls)
    }
    @Test fun cancellationDoesNotBecomeNegativeCache() = runBlocking {
        val fake = Fake().apply { gates["a"] = CompletableDeferred() }
        val store = VideoLinkTitleStore(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { store.resolve("a") }
        job.cancelAndJoin(); fake.gates.clear()
        assertNotNull(store.resolve("a")); assertEquals(2, fake.calls.size)
    }
    @Test fun clearRejectsLateResponseAndAllowsFreshLoad() = runBlocking {
        val fake = Fake().apply { gates["a"] = CompletableDeferred() }
        val store = VideoLinkTitleStore(fake)
        val job = async(start = CoroutineStart.UNDISPATCHED) { store.resolve("a") }
        store.clear(); fake.gates.getValue("a").complete(Unit)
        try { job.await(); fail("stale response") } catch (_: CancellationException) { }
        assertNull(store.cached("a")); assertEquals(1L, store.generation.value)
        assertNotNull(store.resolve("a")); assertEquals(2, fake.calls.size)
    }
    @Test fun cachedTitlesPublishOnLoadAndFreshTitlesWaitForWholeBatch() = runBlocking {
        val one = "BV1234567890"; val two = "BVabcdefghij"; val three = "BV0987654321"
        val fake = Fake(); val store = VideoLinkTitleStore(fake)
        store.resolve(one)
        fake.gates[two] = CompletableDeferred(); fake.gates[three] = CompletableDeferred()
        val holder = LinkTitlesStateHolder("$one $two $three", store)
        assertTrue(holder.state.value.isEmpty())
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        yield()
        assertEquals(setOf(one), holder.state.value.keys)
        fake.gates.getValue(two).complete(Unit); yield()
        assertEquals(setOf(one), holder.state.value.keys)
        fake.gates.getValue(three).complete(Unit); job.join()
        assertEquals(setOf(one, two, three), holder.state.value.keys)
    }
    @Test fun closedHolderNeverPublishesLateTitles() = runBlocking {
        val token = "BV1234567890"
        val fake = Fake().apply { gates[token] = CompletableDeferred() }
        val holder = LinkTitlesStateHolder(token, VideoLinkTitleStore(fake))
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        yield(); holder.close(); fake.gates.getValue(token).complete(Unit); job.join()
        assertTrue(holder.state.value.isEmpty())
    }
    @Test fun oldOrClosedHolderDoesNotStartRequests() = runBlocking {
        val fake = Fake(); val store = VideoLinkTitleStore(fake)
        val stale = LinkTitlesStateHolder("BV1234567890", store)
        store.clear(); stale.load()
        val closed = LinkTitlesStateHolder("BVabcdefghij", store)
        closed.close(); closed.load()
        assertTrue(fake.calls.isEmpty())
    }
}
