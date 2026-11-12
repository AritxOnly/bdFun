package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BackgroundQueueLoaderTest {
    private val detail = VideoDetail("one", 42, "标题", "", "简介", "UP", "", 7, "", 0, 0, 10, 3, 4, 0, 0, 0,
        10, 12, "分区", listOf(VideoPage(12, 1, "一", 10), VideoPage(13, 2, "二", 20)))
    private class Videos : VideoRepository {
        override suspend fun videoDetail(bvid: String): Outcome<VideoDetail> = error("unused")
        override suspend fun related(bvid: String): Outcome<List<VideoItem>> = error("unused")
    }
    private class Streams : VideoPlaybackRepository {
        var gate: CompletableDeferred<Unit>? = null
        val requests = mutableListOf<VideoStreamRequest>()
        override suspend fun playInfo(request: VideoStreamRequest): Outcome<PlayInfo> {
            requests += request; gate?.await()
            return Outcome.Success(PlayInfo(emptyList(), "audio", "fallback", 20000, emptyList()))
        }
        override suspend fun danmaku(cid: Long, durationSec: Int): Outcome<List<DanmakuItem>> = error("unused")
        override suspend fun resumePosition(detail: VideoDetail): Outcome<Long> = error("unused")
    }
    @Test fun nextPageUsesCurrentPreferencesAndAudioTrack() = runBlocking {
        val streams = Streams()
        val track = BackgroundQueueLoader(Videos(), streams).skip(detail, 12, 1, 64, "hevc", "base")!!
        assertEquals(13L, track.cid)
        assertEquals("audio", track.audioUrl)
        assertEquals(20, streams.requests.single().durationSec)
        assertEquals(64, streams.requests.single().quality)
        assertEquals("hevc", streams.requests.single().codecPreference)
    }
    @Test fun edgeOfQueueDoesNotRequestAnotherStream() = runBlocking {
        val streams = Streams()
        assertNull(BackgroundQueueLoader(Videos(), streams).skip(detail, 13, 1, 80, "auto", "base"))
        assertTrue(streams.requests.isEmpty())
    }
    @Test fun exitingInvalidatesPendingSelection() = runBlocking {
        val streams = Streams().apply { gate = CompletableDeferred() }
        val loader = BackgroundQueueLoader(Videos(), streams)
        val result = async(start = CoroutineStart.UNDISPATCHED) { loader.skip(detail, 12, 1, 80, "auto", "base") }
        loader.invalidate(); streams.gate!!.complete(Unit)
        assertNull(result.await())
    }
    @Test fun cancellationIsNotConvertedIntoMissingTrack() = runBlocking {
        val streams = Streams().apply { gate = CompletableDeferred() }
        val loader = BackgroundQueueLoader(Videos(), streams)
        val result = async(start = CoroutineStart.UNDISPATCHED) { loader.skip(detail, 12, 1, 80, "auto", "base") }
        result.cancelAndJoin()
        assertTrue(result.isCancelled)
    }
}
