package com.aritxonly.bdfun.feature.mine.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.LibraryRepository
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.settings.MineLayout
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MinePreviewStateHolderTest {
    private val entry = HistoryItem("BV1", "title", "cover", "author", 0, 25, 100)
    private class Fake : LibraryRepository {
        val calls = mutableListOf<String>()
        var historyResult: Outcome<HistoryPage> = Outcome.Success(HistoryPage(emptyList(), 0, 0, "", false))
        var beforeHistory: suspend () -> Unit = {}
        override suspend fun history(max: Long, viewAt: Long, business: String): Outcome<HistoryPage> {
            calls += "history"; beforeHistory(); return historyResult
        }
        override suspend fun folders(): Outcome<List<FavoriteFolder>> { calls += "favorites"; return Outcome.Success(emptyList()) }
        override suspend fun watchLater(): Outcome<List<HistoryItem>> { calls += "later"; return Outcome.Success(emptyList()) }
        override suspend fun videos(folderId: Long, page: Int): Outcome<Pair<List<VideoItem>, Boolean>> = error("unused")
        override suspend fun enrich(items: List<HistoryItem>): Outcome<List<HistoryItem>> = error("unused")
        override suspend fun removeFavorite(aid: Long, folderId: Long): Outcome<Boolean> = error("unused")
        override suspend fun addFavorite(aid: Long, folderId: Long): Outcome<Boolean> = error("unused")
        override suspend fun deleteHistory(aid: Long): Outcome<Boolean> = error("unused")
        override suspend fun deleteWatchLater(aid: Long): Outcome<Boolean> = error("unused")
    }
    @Test fun collapsedSectionsDoNotRequest() = runBlocking {
        val fake = Fake(); val holder = MinePreviewStateHolder(fake) { true }
        holder.load(MineLayout()); assertTrue(fake.calls.isEmpty())
        holder.load(MineLayout(favorites = true))
        assertEquals(listOf("favorites"), fake.calls)
        assertTrue(holder.state.value[MinePreviewKind.Favorites].loaded)
    }
    @Test fun failureCanRetryAndPreviewIsBounded() = runBlocking {
        val fake = Fake(); fake.historyResult = Outcome.Failure(AppError.Network("offline"))
        val holder = MinePreviewStateHolder(fake) { true }
        holder.load(MineLayout(history = true))
        assertNotNull(holder.state.value[MinePreviewKind.History].error)
        fake.historyResult = Outcome.Success(HistoryPage(List(8) { entry }, 0, 0, "", true))
        holder.retry(MinePreviewKind.History)
        assertEquals(2, holder.state.value[MinePreviewKind.History].entries.size)
        assertNull(holder.state.value[MinePreviewKind.History].error)
    }
    @Test fun accountReplacementRejectsLateResults() = runBlocking {
        val gate = CompletableDeferred<Unit>(); val started = CompletableDeferred<Unit>()
        val fake = Fake(); var owned = true
        fake.beforeHistory = { started.complete(Unit); gate.await() }
        fake.historyResult = Outcome.Success(HistoryPage(listOf(entry), 0, 0, "", false))
        val holder = MinePreviewStateHolder(fake) { owned }
        val job = launch { holder.load(MineLayout(history = true)) }; started.await()
        owned = false; holder.close(); gate.complete(Unit); job.join()
        assertTrue(holder.state.value.sections.isEmpty())
    }
    @Test fun disablingDuringRequestCannotRestoreCard() = runBlocking {
        val gate = CompletableDeferred<Unit>(); val started = CompletableDeferred<Unit>(); val fake = Fake()
        fake.beforeHistory = { started.complete(Unit); gate.await() }
        val holder = MinePreviewStateHolder(fake) { true }
        val job = launch { holder.load(MineLayout(history = true)) }; started.await()
        holder.load(MineLayout()); gate.complete(Unit); job.join()
        assertTrue(holder.state.value.sections.isEmpty())
    }
    @Test fun cancellationReleasesLoadingAndRetryWorks() = runBlocking {
        val started = CompletableDeferred<Unit>(); val fake = Fake()
        fake.beforeHistory = { started.complete(Unit); awaitCancellation() }
        val holder = MinePreviewStateHolder(fake) { true }
        val job = launch { holder.load(MineLayout(history = true)) }; started.await(); job.cancelAndJoin()
        assertFalse(holder.state.value[MinePreviewKind.History].loading)
        fake.beforeHistory = {}; holder.retry(MinePreviewKind.History)
        assertTrue(holder.state.value[MinePreviewKind.History].loaded)
    }
    @Test fun historyResumesAndCompletedOrLaterStartsAtZero() {
        assertEquals(25_000L, entry.mineVideo(true).startPositionMs)
        assertEquals(0L, entry.copy(progress = -1).mineVideo(true).startPositionMs)
        assertEquals(0L, entry.mineVideo(false).startPositionMs)
    }
    @Test fun refreshKeepsVisibleEntriesUntilReplacementArrives() = runBlocking {
        val fake = Fake()
        fake.historyResult = Outcome.Success(HistoryPage(listOf(entry), 0, 0, "", false))
        val holder = MinePreviewStateHolder(fake) { true }
        holder.load(MineLayout(history = true))
        val entered = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        fake.beforeHistory = { entered.complete(Unit); finish.await() }
        fake.historyResult = Outcome.Success(HistoryPage(listOf(entry.copy(title = "new")), 0, 0, "", false))
        val job = launch { holder.load(MineLayout(history = true)) }; entered.await()
        val refreshing = holder.state.value[MinePreviewKind.History]
        assertTrue(refreshing.loading); assertTrue(refreshing.loaded)
        assertEquals(listOf(entry), refreshing.entries)
        finish.complete(Unit); job.join()
        assertEquals("new", holder.state.value[MinePreviewKind.History].entries.single().title)
        assertFalse(holder.state.value[MinePreviewKind.History].loading)
    }
    @Test fun refreshFailureRetainsLastSuccessfulContent() = runBlocking {
        val fake = Fake()
        fake.historyResult = Outcome.Success(HistoryPage(listOf(entry), 0, 0, "", false))
        val holder = MinePreviewStateHolder(fake) { true }
        holder.load(MineLayout(history = true))
        fake.historyResult = Outcome.Failure(AppError.Network("offline"))
        holder.load(MineLayout(history = true))
        val failed = holder.state.value[MinePreviewKind.History]
        assertTrue(failed.loaded); assertFalse(failed.loading)
        assertEquals(listOf(entry), failed.entries); assertNotNull(failed.error)
    }
    @Test fun cancellingRefreshRetainsContentAndUnlocksRetry() = runBlocking {
        val fake = Fake()
        fake.historyResult = Outcome.Success(HistoryPage(listOf(entry), 0, 0, "", false))
        val holder = MinePreviewStateHolder(fake) { true }
        holder.load(MineLayout(history = true))
        val entered = CompletableDeferred<Unit>()
        fake.beforeHistory = { entered.complete(Unit); awaitCancellation() }
        val job = launch { holder.load(MineLayout(history = true)) }; entered.await(); job.cancelAndJoin()
        assertEquals(listOf(entry), holder.state.value[MinePreviewKind.History].entries)
        assertFalse(holder.state.value[MinePreviewKind.History].loading)
        fake.beforeHistory = {}; holder.retry(MinePreviewKind.History)
        assertTrue(holder.state.value[MinePreviewKind.History].loaded)
    }
    @Test fun changedOwnerClearsRetainedContentWithoutRequesting() = runBlocking {
        val fake = Fake(); var owner = true
        fake.historyResult = Outcome.Success(HistoryPage(listOf(entry), 0, 0, "", false))
        val holder = MinePreviewStateHolder(fake) { owner }
        holder.load(MineLayout(history = true)); owner = false
        holder.load(MineLayout(history = true))
        assertTrue(holder.state.value.sections.isEmpty())
        assertEquals(listOf("history"), fake.calls)
    }

}
