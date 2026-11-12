package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class VideoDetailStateHolderTest {
    private val video = VideoItem("one", 42, "one", "", "UP", "", 7, 0, 0, 0, 0)
    private val playback = object : VideoPlaybackRepository {
        override suspend fun playInfo(request: VideoStreamRequest): Outcome<PlayInfo> = Outcome.Failure(AppError.Network("offline"))
        override suspend fun danmaku(cid: Long, durationSec: Int): Outcome<List<DanmakuItem>> = Outcome.Success(emptyList())
        override suspend fun resumePosition(detail: VideoDetail): Outcome<Long> = Outcome.Success(0L)
    }
    private fun holder(repository: VideoRepository, related: List<VideoItem> = emptyList()) =
        VideoDetailStateHolder(repository, related, playback)
    private class Fake : VideoRepository {
        var relatedResult: Outcome<List<VideoItem>> = Outcome.Success(emptyList())
        var detailResult: Outcome<VideoDetail> = Outcome.Failure(AppError.Network("offline"))
        var cancel = false
        val requests = mutableListOf<String>()
        override suspend fun videoDetail(bvid: String): Outcome<VideoDetail> {
            requests += bvid
            if (cancel) throw CancellationException()
            return detailResult
        }
        override suspend fun related(bvid: String): Outcome<List<VideoItem>> {
            requests += bvid
            if (cancel) throw CancellationException()
            return relatedResult
        }
    }

    @Test fun cachedRecommendationsAreAvailableBeforeNetwork() {
        val items = mutableListOf(video)
        val holder = holder(Fake(), items)
        items.clear()
        assertEquals(listOf(video), holder.state.value.related)
    }

    @Test fun failedRefreshDoesNotEraseCachedRecommendations() = runBlocking {
        val repository = Fake().apply { relatedResult = Outcome.Failure(AppError.Network("offline")) }
        val holder = holder(repository, listOf(video))
        assertTrue(holder.fetchRelated("one").isEmpty())
        assertEquals(listOf(video), holder.state.value.related)
    }

    @Test fun offlineRestoreCanClearRecommendationsWithoutNetwork() {
        val repository = Fake()
        val holder = holder(repository, listOf(video))
        holder.restoreRelated(emptyList())
        assertTrue(holder.state.value.related.isEmpty())
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun detailErrorAndRequestedEpisodeArePreserved() = runBlocking {
        val repository = Fake()
        assertEquals(repository.detailResult, holder(repository).fetchDetail("episode"))
        assertEquals(listOf("episode"), repository.requests)
    }

    @Test fun restoringDetailKeepsRecommendationsAndSupportsEpisodeReplacement() {
        val holder = holder(Fake(), listOf(video))
        val detail = VideoDetail(
            "one", 42, "标题", "", "简介", "UP", "", 7, "", 0, 0, 0, 0, 0, 0, 0, 0,
            10, 12, "分区", emptyList(),
        )
        holder.restoreDetail(detail)
        assertEquals(detail, holder.state.value.detail)
        holder.restoreDetail(detail.copy(bvid = "episode", cid = 13))
        assertEquals("episode", holder.state.value.detail?.bvid)
        assertEquals(listOf(video), holder.state.value.related)
    }

    @Test fun cancellationPropagatesForBothReads() = runBlocking {
        val holder = holder(Fake().apply { cancel = true })
        try { holder.fetchDetail("one"); fail("取消必须传播") } catch (_: CancellationException) { }
        try { holder.fetchRelated("one"); fail("取消必须传播") } catch (_: CancellationException) { }
    }
}
