package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.data.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DynamicFeedStateTest {
    private fun item(id: String) = DynamicItem(id, "UP", "", 7, "", "", emptyList(), null, 0, 0, 0)
    private fun page(vararg ids: String, offset: String = "", more: Boolean = false) =
        DynamicPage(ids.map(::item), offset, more)
    private class Source : DynamicFeedSource {
        val requests = mutableListOf<Triple<String, String, Long?>>()
        var feed: suspend (String, String, Long?) -> DynamicPage = { _, _, _ -> DynamicPage(emptyList(), "", false) }
        var portal = DynamicPortal(DynamicUpPage(emptyList(), "", false), emptyList(), 0)
        var ups: suspend (String) -> DynamicUpPage = { DynamicUpPage(emptyList(), "", false) }
        override suspend fun dynamicPage(type: String, offset: String, hostMid: Long?): DynamicPage {
            requests += Triple(type, offset, hostMid)
            return feed(type, offset, hostMid)
        }
        override suspend fun dynamicPortal() = portal
        override suspend fun dynamicUpList(offset: String) = ups(offset)
    }

    @Test fun upRequestUsesHostMidAndNoCategoryOrFixedPage() {
        val params = DynamicFeedContract.feedParams("up", "next", 42)
        assertEquals("42", params["host_mid"])
        assertEquals("next", params["offset"])
        assertFalse(params.containsKey("type"))
        assertFalse(params.containsKey("page"))
        assertTrue(params.getValue("features").contains("opusBigCover"))
        for (type in listOf("all", "video", "pgc", "article")) {
            assertEquals(type, DynamicFeedContract.feedParams(type, "", null)["type"])
        }
    }

    @Test fun categoryCachesKeepServerOrderAndCursor() = runBlocking {
        val source = Source().apply { feed = { type, cursor, _ ->
            if (cursor.isEmpty()) page("$type-2", "$type-1", offset = "$type-next", more = true)
            else page("$type-1", "$type-3")
        } }
        val state = DynamicFeedState(source)
        state.ensureLoaded(true)
        state.switchTo("video", true)
        state.switchTo("all", true)
        assertEquals(2, source.requests.size)
        state.loadMore(true)
        assertEquals("all-next", source.requests.last().second)
        assertEquals(listOf("all-2", "all-1", "all-3"), state.items.map { it.id })
    }

    @Test fun lateAuthorResponseCannotOverwriteNewSelection() = runBlocking {
        val pending = CompletableDeferred<DynamicPage>()
        val source = Source().apply { feed = { _, _, mid -> if (mid == 1L) pending.await() else page("new") } }
        val state = DynamicFeedState(source)
        val old = launch(start = CoroutineStart.UNDISPATCHED) { state.filterTo(1) }
        state.filterTo(2)
        pending.complete(page("old"))
        old.join()
        assertEquals(2L, state.filterMid)
        assertEquals(listOf("new"), state.items.map { it.id })
        assertFalse(state.refreshing)
    }

    @Test fun refreshSelectedAuthorKeepsHostAndResetsCursor() = runBlocking {
        val source = Source().apply { feed = { _, _, _ -> page("one", offset = "next", more = true) } }
        val state = DynamicFeedState(source)
        state.filterTo(7)
        state.loadMore(true)
        state.refresh(true)
        assertEquals(Triple("up", "", 7L), source.requests.last())
        assertEquals("up", state.type)
    }

    @Test fun duplicatePageAndStuckCursorTerminate() = runBlocking {
        val source = Source().apply { feed = { _, _, _ -> page("same", offset = "stuck", more = true) } }
        val state = DynamicFeedState(source)
        state.ensureLoaded(true)
        state.loadMore(true)
        state.loadMore(true)
        assertEquals(2, source.requests.size)
        assertEquals(1, state.items.size)
        assertFalse(state.hasMore)
    }

    @Test fun emptyIntermediatePageContinuesToUsableContent() = runBlocking {
        val source = Source().apply { feed = { _, cursor, _ ->
            if (cursor.isEmpty()) page(offset = "next", more = true) else page("found")
        } }
        val state = DynamicFeedState(source)
        state.ensureLoaded(true)
        assertEquals(listOf("found"), state.items.map { it.id })
    }

    @Test fun emptySuccessfulFeedAndLoggedOutDoNotRepeatedlyFetch() = runBlocking {
        val source = Source()
        val state = DynamicFeedState(source)
        state.ensureLoaded(false)
        assertTrue(source.requests.isEmpty())
        state.ensureLoaded(true)
        state.ensureLoaded(true)
        assertEquals(1, source.requests.size)
    }

    @Test fun failedRefreshRetainsVisibleContentAndCanRetry() = runBlocking {
        val source = Source().apply { feed = { _, _, _ -> page("cached") } }
        val state = DynamicFeedState(source)
        state.ensureLoaded(true)
        source.feed = { _, _, _ -> error("offline") }
        state.refresh(true)
        assertEquals("cached", state.items.single().id)
        assertEquals("offline", state.error)
        assertFalse(state.refreshing)
        source.feed = { _, _, _ -> page("fresh") }
        state.retry(true)
        assertNull(state.error)
        assertEquals("fresh", state.items.single().id)
    }

    @Test fun portalOrderIsIndependentOfFeedAndReadMarkerDoesNotReorder() = runBlocking {
        val source = Source().apply {
            portal = DynamicPortal(DynamicUpPage(listOf(
                DynamicAuthor(9, "Nine", "", true), DynamicAuthor(2, "Two", "", true),
            ), "up-next", true), emptyList(), 0)
            ups = { DynamicUpPage(listOf(DynamicAuthor(2, "Two", ""), DynamicAuthor(3, "Three", "")), "up-next", true) }
        }
        val state = DynamicFeedState(source)
        state.ensureLoaded(true)
        assertTrue(state.items.isEmpty())
        state.markSeen(9)
        assertEquals(listOf(9L, 2L), state.authors.map { it.mid })
        assertFalse(state.authors.first().hasUpdate)
        state.loadMoreAuthors()
        assertEquals(listOf(9L, 2L, 3L), state.authors.map { it.mid })
        assertFalse(state.authorsHasMore)
    }

    @Test fun refreshWinsOverOutstandingPagination() = runBlocking {
        val pending = CompletableDeferred<DynamicPage>()
        val source = Source().apply { feed = { _, cursor, _ ->
            if (cursor.isEmpty()) page("first", offset = "old-next", more = true) else pending.await()
        } }
        val state = DynamicFeedState(source)
        state.ensureLoaded(true)
        val load = launch(start = CoroutineStart.UNDISPATCHED) { state.loadMore(true) }
        source.feed = { _, _, _ -> page("fresh") }
        state.refresh(true)
        pending.complete(page("stale"))
        load.join()
        assertEquals(listOf("fresh"), state.items.map { it.id })
    }
}
