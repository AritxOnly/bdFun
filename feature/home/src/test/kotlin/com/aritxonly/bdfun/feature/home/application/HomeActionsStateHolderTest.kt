package com.aritxonly.bdfun.feature.home.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.feed.DynamicPage
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HomeActionsStateHolderTest {
    private val video = VideoItem("one", 42, "one", "", "UP", "", 7, 0, 0, 0, 0, categoryName = "游戏")
    private class Actions : RecommendationActionsRepository {
        val reasons = mutableListOf<RecommendationReason>()
        var fail = false
        var watch: suspend () -> Outcome<Boolean> = { Outcome.Success(true) }
        override suspend fun addWatchLater(aid: Long) = watch()
        override suspend fun dislike(video: VideoItem, reason: RecommendationReason): Outcome<Unit> {
            reasons += reason
            return if (fail) Outcome.Failure(AppError.Network("offline")) else Outcome.Success(Unit)
        }
    }
    private val videos = object : VideoRepository {
        override suspend fun videoDetail(bvid: String): Outcome<VideoDetail> = Outcome.Failure(AppError.Network("offline"))
        override suspend fun related(bvid: String): Outcome<List<VideoItem>> = Outcome.Success(emptyList())
    }
    private fun feed(vararg items: VideoItem) = HomeFeedStateHolder(object : FeedRepository {
        override suspend fun recommend(page: Int): Outcome<List<VideoItem>> = Outcome.Success(items.toList())
        override suspend fun dynamics(offset: String): Outcome<DynamicPage> = error("unused")
    }, videos)

    @Test fun webFeedbackUsesExistingReasonAndOnlyThenFilters() = runBlocking {
        val feed = feed(video)
        feed.ensureLoaded()
        val actions = Actions()
        val state = HomeActionsStateHolder(videos, actions, feed)
        state.select(video)
        assertEquals(Outcome.Success("已反馈，减少此类推荐"), state.perform(HomeVideoAction.HideVideo))
        assertEquals(1L, actions.reasons.single().id)
        assertTrue(feed.state.value.videos.isEmpty())
        assertFalse(state.state.value.busy)
    }
    @Test fun failedFeedbackLeavesVisibleVideoAndCanRetry() = runBlocking {
        val feed = feed(video); feed.ensureLoaded()
        val actions = Actions().apply { fail = true }
        val state = HomeActionsStateHolder(videos, actions, feed)
        state.select(video)
        assertEquals(Outcome.Failure(AppError.Network("offline")), state.perform(HomeVideoAction.HideCreator))
        assertEquals(video, feed.state.value.videos.single())
        actions.fail = false
        state.perform(HomeVideoAction.HideCreator)
        assertTrue(feed.state.value.videos.isEmpty())
        assertEquals(listOf(4L, 4L), actions.reasons.map { it.id })
    }
    @Test fun appFeedbackNeverInventsMissingServerReasons() = runBlocking {
        val target = video.copy(appRecommendation = true)
        val feed = feed(target); feed.ensureLoaded()
        val actions = Actions()
        val state = HomeActionsStateHolder(videos, actions, feed)
        state.select(target)
        assertTrue(state.perform(HomeVideoAction.HideVideo) is Outcome.Failure)
        assertTrue(actions.reasons.isEmpty())
        assertEquals(target, feed.state.value.videos.single())
    }
    @Test fun categoryWithoutServerReasonIsFilteredOnlyForThisSession() = runBlocking {
        val feed = feed(video); feed.ensureLoaded()
        val actions = Actions()
        val state = HomeActionsStateHolder(videos, actions, feed)
        state.select(video)
        assertEquals(Outcome.Success("本次浏览不再显示 游戏 推荐"), state.perform(HomeVideoAction.HideCategory))
        assertTrue(actions.reasons.isEmpty())
        assertTrue(feed.state.value.videos.isEmpty())
    }
    @Test fun unavailableCategoryStaysUnknownAndDoesNotCreateFeedback() = runBlocking {
        val state = HomeActionsStateHolder(videos, Actions(), feed())
        state.select(video.copy(categoryName = ""))
        assertEquals("", state.state.value.category)
        assertFalse(state.state.value.categoryLoading)
        assertTrue(state.perform(HomeVideoAction.HideCategory) is Outcome.Failure)
    }
    @Test fun pendingActionCannotBeSubmittedTwiceAndCancellationUnlocksIt() = runBlocking {
        val pending = CompletableDeferred<Outcome<Boolean>>()
        var calls = 0
        val actions = Actions().apply { watch = { calls++; pending.await() } }
        val state = HomeActionsStateHolder(videos, actions, feed())
        state.select(video)
        val running = launch(start = CoroutineStart.UNDISPATCHED) { state.perform(HomeVideoAction.WatchLater) }
        assertTrue(state.state.value.busy)
        assertNull(state.perform(HomeVideoAction.WatchLater))
        assertEquals(1, calls)
        running.cancelAndJoin()
        assertFalse(state.state.value.busy)
    }

    @Test fun closeRejectsLateWatchLaterToastAndFutureActions() = runBlocking {
        val pending = CompletableDeferred<Outcome<Boolean>>()
        val actions = Actions().apply { watch = { pending.await() } }
        val state = HomeActionsStateHolder(videos, actions, feed(video)); state.select(video)
        val request = async(start = CoroutineStart.UNDISPATCHED) { state.perform(HomeVideoAction.WatchLater) }
        state.close(); pending.complete(Outcome.Success(true))
        assertNull(request.await()); assertFalse(state.state.value.busy)
        state.select(video.copy(bvid = "other"))
        assertEquals(video, state.state.value.video); assertNull(state.perform(HomeVideoAction.WatchLater))
    }
    @Test fun closeRejectsLateCategoryResponseAndReleasesLoading() = runBlocking {
        val pending = CompletableDeferred<Outcome<VideoDetail>>()
        val repository = object : VideoRepository {
            override suspend fun videoDetail(bvid: String) = pending.await()
            override suspend fun related(bvid: String): Outcome<List<VideoItem>> = error("unused")
        }
        val state = HomeActionsStateHolder(repository, Actions(), feed(video))
        val request = launch(start = CoroutineStart.UNDISPATCHED) { state.select(video.copy(categoryName = "")) }
        state.close(); pending.complete(Outcome.Success(VideoDetail(
            "one", 42, "one", "", "", "UP", "", 7, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "迟到分类", emptyList()))); request.join()
        assertFalse(state.state.value.categoryLoading); assertEquals("", state.state.value.category)
    }
    @Test fun closePreventsLateDislikeFromFilteringFeed() = runBlocking {
        val pending = CompletableDeferred<Outcome<Unit>>()
        val actions = object : RecommendationActionsRepository {
            override suspend fun addWatchLater(aid: Long): Outcome<Boolean> = error("unused")
            override suspend fun dislike(video: VideoItem, reason: RecommendationReason) = pending.await()
        }
        val feed = feed(video); feed.ensureLoaded()
        val state = HomeActionsStateHolder(videos, actions, feed); state.select(video)
        val request = async(start = CoroutineStart.UNDISPATCHED) { state.perform(HomeVideoAction.HideVideo) }
        state.close(); pending.complete(Outcome.Success(Unit))
        try { request.await(); fail("closed action must cancel") } catch (_: CancellationException) { }
        assertEquals(listOf(video), feed.state.value.videos); assertFalse(state.state.value.busy)
    }
}
