package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CacheInitializationTest {
    @Test fun deleteWhileReadingCannotRestoreDeletedEntryOrPersistItForNextLaunch() = runBlocking {
        val entered = CountDownLatch(1); val release = CountDownLatch(1); val persisted = CompletableDeferred<Unit>()
        val files = object : CacheFilesFake() {
            override fun read(): List<com.aritxonly.bdfun.core.model.cache.CachedVideo> {
                entered.countDown(); check(release.await(5, TimeUnit.SECONDS)); return listOf(cacheEntry())
            }
            override fun write(entries: List<com.aritxonly.bdfun.core.model.cache.CachedVideo>) { super.write(entries); persisted.complete(Unit) }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val queue = cacheQueue(files, CacheTransferFake(files), scope); queue.initialize()
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS)); queue.delete(cacheEntry().id); release.countDown()
            withTimeout(5000) { persisted.await() }
            assertTrue(queue.items.isEmpty()); assertTrue(files.written.last().isEmpty())
        } finally { release.countDown(); queue.close(); scope.cancel() }
    }
    @Test fun lateInitialReadMergesWithoutOverwritingAlreadyQueuedSameId() = runBlocking {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val files = object : CacheFilesFake() {
            override fun read(): List<com.aritxonly.bdfun.core.model.cache.CachedVideo> {
                entered.countDown(); check(release.await(5, TimeUnit.SECONDS)); return listOf(cacheEntry(), cacheEntry(id = "other"))
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val transfer = CacheTransferFake(files).apply { gate = CompletableDeferred() }
        val queue = cacheQueue(files, transfer, scope); queue.initialize()
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            queue.enqueue(cacheDetail().copy(title = "new"), cacheQuality(), 7, "audio", 12)
            release.countDown(); withTimeout(5000) { queue.state.first { it.any { row -> row.id == "other" } } }
            assertEquals(2, queue.items.size); assertEquals("new", queue.items.first().title)
            assertEquals(1, queue.items.count { it.id == cacheEntry().id })
        } finally { release.countDown(); queue.close(); scope.cancel() }
    }
}
