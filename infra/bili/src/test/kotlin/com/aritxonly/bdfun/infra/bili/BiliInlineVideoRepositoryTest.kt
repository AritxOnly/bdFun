package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.InlineVideoPreferences
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class BiliInlineVideoRepositoryTest {
    private val preferences = InlineVideoPreferences(64, "auto", "base")
    private fun detail(id: String) = VideoDetail(
        bvid = id, aid = 1, title = "", cover = "", desc = "", ownerName = "", ownerFace = "", ownerMid = 1,
        ownerSign = "", play = 0, danmaku = 0, like = 0, coin = 0, favorite = 0, share = 0, reply = 0,
        pubdate = 0, duration = 1, cid = 2, tname = "", pages = emptyList(),
    )
    private fun info(id: String = "one") = PlayInfo(emptyList(), null, id, 1, emptyList())

    @Test fun cachedInfoRetainsBvidOnlyKeyAndSkipsChangedPreferences() = runBlocking {
        val views = AtomicInteger(); val plays = AtomicInteger()
        val repository = BiliInlineVideoRepository({ views.incrementAndGet(); detail(it) }, { _, _ -> plays.incrementAndGet(); info() })
        assertNull(repository.cachedPlayInfo("one"))
        assertEquals(Outcome.Success(info()), repository.load("one") { preferences })
        assertEquals(info(), repository.cachedPlayInfo("one"))
        assertEquals(Outcome.Success(info()), repository.load("one") { error("命中后不能重读设置") })
        assertEquals(1, views.get()); assertEquals(1, plays.get())
    }

    @Test fun playbackFailureRetainsSuccessfulDetailForReentry() = runBlocking {
        val views = AtomicInteger(); val plays = AtomicInteger()
        val repository = BiliInlineVideoRepository({ views.incrementAndGet(); detail(it) }, { _, _ ->
            if (plays.incrementAndGet() == 1) error("offline") else info()
        })
        assertTrue(repository.load("one") { preferences } is Outcome.Failure)
        assertNull(repository.cachedPlayInfo("one"))
        assertEquals(Outcome.Success(info()), repository.load("one") { preferences })
        assertEquals(1, views.get()); assertEquals(2, plays.get())
    }

    @Test fun preferencesAreSampledAfterDetailCompletes() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val gate = CompletableDeferred<Unit>()
        var sampled: InlineVideoPreferences? = null
        val repository = BiliInlineVideoRepository({ entered.complete(Unit); gate.await(); detail(it) }, { _, prefs -> sampled = prefs; info() })
        var current = preferences
        val running = async { repository.load("one") { current } }
        entered.await(); assertNull(sampled)
        current = InlineVideoPreferences(32, "hevc", "other"); gate.complete(Unit)
        assertEquals(Outcome.Success(info()), running.await()); assertEquals(current, sampled)
    }

    @Test fun parallelPreviewCanFillPlaybackCacheWhileFirstDetailIsPending() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val gate = CompletableDeferred<Unit>()
        val views = AtomicInteger(); val firstSamples = AtomicInteger(); val plays = AtomicInteger()
        val repository = BiliInlineVideoRepository({ id ->
            if (views.incrementAndGet() == 1) { entered.complete(Unit); gate.await() }
            detail(id)
        }, { _, _ -> plays.incrementAndGet(); info() })
        val first = async { repository.load("one") { firstSamples.incrementAndGet(); preferences } }
        entered.await()
        assertEquals(Outcome.Success(info()), repository.load("one") { preferences })
        gate.complete(Unit); assertEquals(Outcome.Success(info()), first.await())
        assertEquals(2, views.get()); assertEquals(1, plays.get()); assertEquals(0, firstSamples.get())
    }

    @Test fun processCacheRetainsTwelveEntriesAndReloadsAnEvictedOne() = runBlocking {
        val views = AtomicInteger(); val plays = AtomicInteger()
        val repository = BiliInlineVideoRepository({ views.incrementAndGet(); detail(it) }, { detail, _ -> plays.incrementAndGet(); info(detail.bvid) })
        val ids = (1..13).map { "BV$it" }
        for (id in ids) assertTrue(repository.load(id) { preferences } is Outcome.Success)
        assertEquals(12, ids.count { repository.cachedPlayInfo(it) != null })
        val evicted = ids.single { repository.cachedPlayInfo(it) == null }
        assertEquals(Outcome.Success(info(evicted)), repository.load(evicted) { preferences })
        assertEquals(14, plays.get()); assertEquals(12, ids.count { repository.cachedPlayInfo(it) != null })
        assertTrue(views.get() >= 13)
    }

    @Test fun cancellationLeavesNoFailedCacheAndPropagates() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val gate = CompletableDeferred<Unit>()
        val repository = BiliInlineVideoRepository({ entered.complete(Unit); gate.await(); detail(it) }, { _, _ -> info() })
        val running = async { repository.load("one") { preferences } }
        entered.await(); running.cancelAndJoin()
        assertTrue(running.isCancelled); assertNull(repository.cachedPlayInfo("one"))
    }
}
