package com.aritxonly.bdfun.feature.library.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.LibraryRepository
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LibraryStateHolderTest {
    private fun video(id: Long) = VideoItem("BV$id", id, "标题", "", "UP", "", 1, 0, 0, 1, 0)
    private fun history(id: Long) = HistoryItem("BV$id", "标题", "", "UP", 0, 0, 10, aid = id)
    private class Fake : LibraryRepository {
        var videos = emptyList<VideoItem>()
        var history = HistoryPage(emptyList(), 0, 0, "", false)
        var later = emptyList<HistoryItem>()
        var success = true
        val removed = mutableListOf<Pair<Long, Long>>()
        val added = mutableListOf<Pair<Long, Long>>()
        var requestedCursor: Triple<Long, Long, String>? = null
        var loadingVideo: (suspend (Long) -> Unit)? = null
        override suspend fun folders() = Outcome.Success(listOf(FavoriteFolder(1, "原夹", 2, ""), FavoriteFolder(2, "目标夹", 2, "")))
        override suspend fun videos(folderId: Long, page: Int): Outcome<Pair<List<VideoItem>, Boolean>> {
            loadingVideo?.invoke(folderId)
            return Outcome.Success(videos to (page == 1))
        }
        override suspend fun history(max: Long, viewAt: Long, business: String): Outcome<HistoryPage> {
            requestedCursor = Triple(max, viewAt, business); return Outcome.Success(history)
        }
        override suspend fun enrich(items: List<HistoryItem>) = Outcome.Failure(AppError.Network("offline"))
        override suspend fun watchLater() = Outcome.Success(later)
        override suspend fun removeFavorite(aid: Long, folderId: Long): Outcome<Boolean> {
            removed += aid to folderId; return Outcome.Success(success)
        }
        override suspend fun addFavorite(aid: Long, folderId: Long): Outcome<Boolean> {
            added += aid to folderId; return Outcome.Success(success)
        }
        override suspend fun deleteHistory(aid: Long) = Outcome.Success(success)
        override suspend fun deleteWatchLater(aid: Long) = Outcome.Success(success)
    }

    @Test fun failedDeletesStayVisibleAcrossAllThreeTabs() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1)); source.later = listOf(history(1))
        source.history = HistoryPage(source.later, 10, 20, "archive", true)
        val holder = LibraryStateHolder(source)
        holder.loadFavorites(); holder.loadHistory(); holder.loadWatchLater(); source.success = false
        for (tab in 0..2) assertEquals("部分删除失败，请重试", holder.deleteSelected(tab, setOf("BV1")))
        assertEquals(1, holder.state.value.favorites.videos.size)
        assertEquals(1, holder.state.value.history.entries.size)
        assertEquals(1, holder.state.value.watchLater.size)
        assertFalse(holder.state.value.busy)
    }

    @Test fun successfulDeleteRemovesOnlySelectedItem() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1), video(2))
        val holder = LibraryStateHolder(source); holder.loadFavorites()
        assertEquals("已删除 1 项", holder.deleteSelected(0, setOf("BV1")))
        assertEquals(listOf("BV2"), holder.state.value.favorites.videos.map { it.bvid })
        assertEquals(listOf(1L to 1L), source.removed)
    }

    @Test fun failedMoveKeepsSourceAndNeverDeletesAfterFailedAdd() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1))
        val holder = LibraryStateHolder(source); holder.loadFavorites(); source.success = false
        holder.transferSelected(true, FavoriteFolder(2, "目标夹", 0, ""), setOf("BV1"))
        assertTrue(source.removed.isEmpty())
        assertEquals(1, holder.state.value.favorites.videos.size)
    }

    @Test fun moveIntoSameFolderCannotDeleteSource() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1))
        val holder = LibraryStateHolder(source); holder.loadFavorites()
        assertEquals("已在「原夹」中", holder.transferSelected(true, FavoriteFolder(1, "原夹", 1, ""), setOf("BV1")))
        assertTrue(source.added.isEmpty()); assertTrue(source.removed.isEmpty())
        assertEquals(1, holder.state.value.favorites.videos.size)
    }

    @Test fun historyKeepsAllCursorFieldsAndStopsOnDuplicatePage() = runBlocking {
        val source = Fake(); source.history = HistoryPage(listOf(history(1)), 10, 20, "archive", true)
        val holder = LibraryStateHolder(source); holder.loadHistory(); holder.loadMoreHistory()
        assertEquals(Triple(10L, 20L, "archive"), source.requestedCursor)
        assertFalse(holder.state.value.history.hasMore)
        assertEquals(1, holder.state.value.history.entries.size)
    }

    @Test fun staleFolderResponseCannotReplaceNewSelection() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1))
        val holder = LibraryStateHolder(source); holder.loadFavorites()
        val gate = CompletableDeferred<Unit>()
        source.loadingVideo = { if (it == 1L) gate.await() }
        val old = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadFolder(0) }
        source.videos = listOf(video(2)); holder.loadFolder(1)
        source.videos = listOf(video(1)); gate.complete(Unit); old.join()
        assertEquals(1, holder.state.value.favorites.index)
        assertEquals(listOf("BV2"), holder.state.value.favorites.videos.map { it.bvid })
        assertFalse(holder.state.value.loading[0] == true)
    }

    @Test fun cancellationDoesNotLeavePaginationLocked() = runBlocking {
        val source = Fake(); source.videos = listOf(video(1))
        val holder = LibraryStateHolder(source); holder.loadFavorites()
        source.loadingVideo = { awaitCancellation() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadMoreFolders() }
        job.cancelAndJoin()
        assertFalse(holder.state.value.favorites.loadingMore)
        assertTrue(holder.state.value.favorites.hasMore)
    }
}
