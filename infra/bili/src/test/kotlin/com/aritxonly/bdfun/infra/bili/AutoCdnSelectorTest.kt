package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.AutoCdnCache
import com.aritxonly.bdfun.core.model.settings.AutoCdnSnapshot
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AutoCdnSelectorTest {
    private val candidates = listOf(CdnService("auto", "自动", auto = true), CdnService("base", "原地址"),
        CdnService("backup", "备用", useBackup = true), CdnService("a", "A", "a.test"), CdnService("b", "B", "b.test"))
    private class Cache(var value: AutoCdnSnapshot = AutoCdnSnapshot()) : AutoCdnCache {
        var writes = 0
        override fun read() = value
        override fun write(snapshot: AutoCdnSnapshot) { value = snapshot; writes++ }
    }
    @Test fun cachedWinnerAndExactOneHourBoundaryArePreserved() = runBlocking {
        val cache = Cache(AutoCdnSnapshot("b", 2.5, 100, mapOf("b" to 2.5)))
        var now = 3_600_099L; var samples = 0
        val selector = AutoCdnSelector({ samples++; "sample" }, cache, { _, _ -> }, { _, _ -> 1.0 }, { now }, candidates)
        assertEquals("b", selector.currentServiceId()); assertEquals("B · 2.5 MB/s", selector.describe())
        assertTrue(selector.isFresh()); assertTrue(selector.refreshSelection()); assertEquals(0, samples)
        now++; assertFalse(selector.isFresh()); selector.refreshSelection(); assertEquals(1, samples)
    }
    @Test fun selectsOnlyEligibleNodesAndKeepsFirstCandidateOnTie() = runBlocking {
        val probed = java.util.Collections.synchronizedList(mutableListOf<String>()); val cache = Cache()
        val selector = AutoCdnSelector({ "sample" }, cache, { _, _ -> }, { service, sample ->
            assertEquals("sample", sample); probed += service.id; 3.0
        }, { 123L }, candidates)
        assertTrue(selector.refreshSelection()); assertEquals(setOf("a", "b"), probed.toSet())
        assertEquals("a", selector.currentServiceId()); assertEquals(mapOf("a" to 3.0, "b" to 3.0), selector.speeds())
        assertEquals(123L, cache.value.measuredAt); assertEquals(1, cache.writes)
    }
    @Test fun forcedRefreshBypassesCacheAndFailedSampleKeepsPreviousSnapshot() = runBlocking {
        val original = AutoCdnSnapshot("b", 2.5, 100, mapOf("b" to 2.5)); val cache = Cache(original)
        val selector = AutoCdnSelector({ null }, cache, { _, _ -> }, { _, _ -> error("no sample") }, { 101L }, candidates)
        assertFalse(selector.refreshSelection(true)); assertFalse(selector.isProbing())
        assertEquals("b", selector.currentServiceId()); assertEquals(original.speeds, selector.speeds())
        assertEquals(0, cache.writes); assertEquals("B · 2.5 MB/s", selector.describe())
    }
    @Test fun noProbeResultsClearScoresButKeepCachedWinner() = runBlocking {
        val cache = Cache(AutoCdnSnapshot("b", 2.5, 100, mapOf("b" to 2.5)))
        val selector = AutoCdnSelector({ "sample" }, cache, { _, _ -> }, { _, _ -> null }, { 101L }, candidates)
        assertFalse(selector.refreshSelection(true)); assertTrue(selector.speeds().isEmpty())
        assertEquals("b", selector.currentServiceId()); assertEquals(0, cache.writes)
    }
    @Test fun invalidCachedNodeIsFreshByOriginalIdRuleButDoesNotResolve() = runBlocking {
        val selector = AutoCdnSelector({ null }, Cache(AutoCdnSnapshot("missing", measuredAt = 100)),
            { _, _ -> }, { _, _ -> null }, { 101L }, candidates)
        assertTrue(selector.isFresh()); assertNull(selector.currentServiceId())
        assertEquals("自动测速尚未完成，暂用备用地址", selector.describe())
        selector.refreshSelection(true); assertEquals("自动测速失败，暂用备用地址", selector.describe())
    }
    @Test fun cancellationReleasesProbingAndMutexForRetry() = runBlocking {
        var gate: CompletableDeferred<Unit>? = CompletableDeferred()
        val selector = AutoCdnSelector({ gate?.await(); "sample" }, Cache(), { _, _ -> }, { _, _ -> 1.0 }, { 0L }, candidates)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { selector.refreshSelection() }
        assertTrue(selector.isProbing()); job.cancelAndJoin(); assertFalse(selector.isProbing())
        gate = null; assertTrue(selector.refreshSelection()); assertFalse(selector.isProbing())
    }
    @Test fun probesAtMostFourNodesInParallel() = runBlocking {
        val entered = CountDownLatch(4); val release = CountDownLatch(1)
        val active = AtomicInteger(); val maximum = AtomicInteger()
        val nodes = (1..8).map { CdnService("$it", "$it", "node$it.test") }
        val selector = AutoCdnSelector({ "sample" }, Cache(), { _, _ -> }, { _, _ ->
            val count = active.incrementAndGet(); maximum.accumulateAndGet(count, ::maxOf); entered.countDown()
            try { check(release.await(5, TimeUnit.SECONDS)); 1.0 } finally { active.decrementAndGet() }
        }, { 0L }, nodes)
        val job = async(Dispatchers.Default) { selector.refreshSelection() }
        try { assertTrue(entered.await(5, TimeUnit.SECONDS)); assertEquals(4, maximum.get()) }
        finally { release.countDown() }
        assertTrue(job.await()); assertEquals(4, maximum.get()); assertEquals(8, selector.speeds().size)
    }
}
