package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner

import com.aritxonly.bdfun.core.model.video.VideoDetailInfo

import com.aritxonly.bdfun.core.model.video.VideoDetailCounts

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.VideoEntryRepository
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VideoEntryStateHolderTest {
    private val detail = VideoDetail(
        bvid = "resolved",
        aid = 42,
        title = "详情",
        cover = "cover",
        desc = "desc",
        owner = VideoDetailOwner(
            ownerName = "UP",
            ownerFace = "face",
            ownerMid = 7,
            ownerSign = "sign",
        ),
        counts = VideoDetailCounts(
            play = 1234,
            danmaku = 555,
            like = 3,
            coin = 4,
            favorite = 5,
            share = 6,
            reply = 7,
        ),
        info = VideoDetailInfo(
            pubdate = 123456,
            duration = 88,
            cid = 99,
            tname = "分区",
            pages = emptyList(),
        ),
    )
    private class Fake(val load: suspend (String) -> Outcome<VideoDetail>) : VideoEntryRepository {
        val calls = mutableListOf<String>()
        override suspend fun byBvid(bvid: String): Outcome<VideoDetail> {
            calls += "bv:$bvid"; return load("bv:$bvid")
        }
        override suspend fun byAid(aid: Long): Outcome<VideoDetail> {
            calls += "av:$aid"; return load("av:$aid")
        }
    }
    @Test fun routeSnapshotSkipsBothReadsAndKeepsExplicitProgress() = runBlocking {
        val repository = Fake { error("snapshot must not fetch") }
        val snapshot = VideoItem(
            bvid = "cached",
            aid = 1,
            title = "缓存",
            cover = "",
            ownerName = "UP",
            ownerFace = "",
            ownerMid = 7,
            play = 0,
            danmaku = 0,
            duration = 88,
            pubdate = 0,
            extras = VideoItemExtras(
                startPositionMs = 12000,
            ),
        )
        val holder = VideoEntryStateHolder(repository, snapshot)
        assertFalse(holder.state.value.loading)
        assertEquals(VideoEntryResolution.Ready, holder.resolve("different", 42))
        assertEquals(snapshot, holder.state.value.video)
        assertTrue(repository.calls.isEmpty())
    }
    @Test fun bvidWinsOverAidAndOriginalDetailFieldsReachListSnapshot() = runBlocking {
        val repository = Fake { Outcome.Success(detail) }
        val holder = VideoEntryStateHolder(repository)
        assertEquals(VideoEntryResolution.Ready, holder.resolve("input", 55))
        assertEquals(listOf("bv:input"), repository.calls)
        val video = checkNotNull(holder.state.value.video)
        assertEquals("resolved", video.bvid)
        assertEquals("详情", video.title)
        assertEquals(1234L, video.play)
        assertEquals(555L, video.danmaku)
        assertEquals(88, video.duration)
        assertEquals(99L, video.cid)
        assertEquals(123456L, video.pubdate)
        assertFalse(holder.state.value.loading)
    }
    @Test fun blankBvidFallsBackToPositiveAid() = runBlocking {
        val repository = Fake { Outcome.Success(detail) }
        val holder = VideoEntryStateHolder(repository)
        assertEquals(VideoEntryResolution.Ready, holder.resolve(" ", 42))
        assertEquals(listOf("av:42"), repository.calls)
    }
    @Test fun missingIdentifierAndReadFailureKeepOriginalCloseAction() = runBlocking {
        val repository = Fake { Outcome.Failure(AppError.Network("offline")) }
        val missing = VideoEntryStateHolder(repository)
        assertEquals(VideoEntryResolution.Close, missing.resolve(null, 0))
        assertTrue(repository.calls.isEmpty())
        val failed = VideoEntryStateHolder(repository)
        assertEquals(VideoEntryResolution.Close, failed.resolve("input", 0))
        assertEquals(AppError.Network("offline"), failed.state.value.error)
        assertNull(failed.state.value.video)
        assertFalse(failed.state.value.loading)
    }
    @Test fun cancellationPropagatesAndReleasesLoadingWithoutClosing() = runBlocking {
        val repository = Fake { throw CancellationException("cancelled") }
        val holder = VideoEntryStateHolder(repository)
        try { holder.resolve("input", 0); fail("cancellation must propagate") }
        catch (_: CancellationException) { }
        assertNull(holder.state.value.video)
        assertNull(holder.state.value.error)
        assertFalse(holder.state.value.loading)
    }
    @Test fun olderFailureCannotCloseOrOverwriteResolvedNewEntry() = runBlocking {
        val old = CompletableDeferred<Outcome<VideoDetail>>()
        val repository = Fake { if (it == "bv:old") old.await() else Outcome.Success(detail) }
        val holder = VideoEntryStateHolder(repository)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { holder.resolve("old", 0) }
        assertEquals(VideoEntryResolution.Ready, holder.resolve("new", 0))
        old.complete(Outcome.Failure(AppError.Network("old failure")))
        assertEquals(VideoEntryResolution.Obsolete, pending.await())
        assertEquals("resolved", holder.state.value.video?.bvid)
        assertNull(holder.state.value.error)
    }
}
