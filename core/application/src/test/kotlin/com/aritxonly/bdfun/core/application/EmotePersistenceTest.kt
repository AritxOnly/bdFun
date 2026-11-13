package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class EmotePersistenceTest {
    private val items = listOf(EmoteItem("doge", "url", 2, "icon", 1, 2))
    private class Cache : EmoteCache {
        val entries = mutableMapOf<Long, CachedEmotes>()
        var readGate: CompletableDeferred<Unit>? = null
        var broken = false
        override suspend fun read(accountId: Long): Outcome<CachedEmotes?> {
            readGate?.await()
            return if (broken) Outcome.Failure(AppError.Parsing("broken")) else Outcome.Success(entries[accountId])
        }
        override suspend fun write(accountId: Long, snapshot: CachedEmotes): Outcome<Unit> {
            entries[accountId] = snapshot
            return Outcome.Success(Unit)
        }
    }
    @Test fun restartReusesFreshAccountMetadataWithoutNetwork() = runBlocking {
        val cache = Cache()
        var calls = 0
        val repository = object : EmoteRepository {
            override suspend fun replyEmotes(): Outcome<List<EmoteItem>> { calls++; return Outcome.Success(items) }
        }
        EmoteStore(repository, cache, { 7 }, { 100 }).ensure()
        val restarted = EmoteStore(repository, cache, { 7 }, { 101 })
        restarted.ensure()
        assertEquals(1, calls)
        assertEquals(items, restarted.state.value.items)
        assertFalse(restarted.state.value.loading)
    }
    @Test fun staleCacheIsVisibleDuringRefreshAndSurvivesOfflineFailure() = runBlocking {
        val cache = Cache().apply { entries[7] = CachedEmotes(items, 0) }
        val gate = CompletableDeferred<Unit>()
        val repository = object : EmoteRepository {
            override suspend fun replyEmotes(): Outcome<List<EmoteItem>> {
                gate.await(); return Outcome.Failure(AppError.Network("offline"))
            }
        }
        val store = EmoteStore(repository, cache, { 7 }, { 90_000_000 })
        val request = launch(start = CoroutineStart.UNDISPATCHED) { store.ensure() }
        assertEquals(items, store.state.value.items)
        assertTrue(store.state.value.loading)
        gate.complete(Unit); request.join()
        assertEquals(items, store.state.value.items)
        assertFalse(store.state.value.loading)
        assertNotNull(store.state.value.error)
    }
    @Test fun anotherAccountCannotSeeCachedPackAndLateDiskReadIsRejected() = runBlocking {
        val cache = Cache().apply { entries[7] = CachedEmotes(items, 100); readGate = CompletableDeferred() }
        var owner = 7L
        val repository = object : EmoteRepository {
            override suspend fun replyEmotes(): Outcome<List<EmoteItem>> = Outcome.Success(emptyList())
        }
        val store = EmoteStore(repository, cache, { owner }, { 101 })
        val request = launch(start = CoroutineStart.UNDISPATCHED) { store.ensure() }
        owner = 8; store.clear(); cache.readGate!!.complete(Unit); request.join()
        assertTrue(store.state.value.items.isEmpty())
        store.ensure()
        assertTrue(store.state.value.items.isEmpty())
    }
    @Test fun corruptCacheFallsBackToNetworkAndCancellationUnlocksDiskRead() = runBlocking {
        val cache = Cache().apply { readGate = CompletableDeferred() }
        val repository = object : EmoteRepository {
            override suspend fun replyEmotes(): Outcome<List<EmoteItem>> = Outcome.Success(items)
        }
        val store = EmoteStore(repository, cache)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { store.ensure() }
        job.cancelAndJoin()
        assertFalse(store.state.value.loading)
        cache.readGate = null; cache.broken = true
        store.ensure()
        assertEquals(items, store.state.value.items)
    }
}
