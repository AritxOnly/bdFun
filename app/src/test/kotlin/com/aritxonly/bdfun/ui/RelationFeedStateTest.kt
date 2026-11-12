package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RelationFeedStateTest {
    private fun RelationFeedState(load: suspend (Int) -> RelationPage) =
        com.aritxonly.bdfun.feature.relations.application.RelationFeedStateHolder(
            object : com.aritxonly.bdfun.core.domain.RelationRepository {
                override suspend fun users(mid: Long, fans: Boolean, page: Int, frequent: Boolean): com.aritxonly.bdfun.core.common.Outcome<RelationPage> = try {
                    com.aritxonly.bdfun.core.common.Outcome.Success(load(page))
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { com.aritxonly.bdfun.core.common.Outcome.Failure(com.aritxonly.bdfun.core.common.AppError.Unknown(failure.message ?: "加载失败，请重试")) }
                override suspend fun attribute(mid: Long): com.aritxonly.bdfun.core.common.Outcome<Int?> = error("unused")
                override suspend fun follow(mid: Long, followed: Boolean): com.aritxonly.bdfun.core.common.Outcome<Unit> = error("unused")
            }, 1L, false, false, null, { null }, { false })
    private fun user(id: Long) = RelationUser(id, "user$id", "", "", 0)
    @Test fun failedNextPageRetriesSameCursorAndDeduplicatesOverlappingUsers() = runBlocking {
        val calls = mutableListOf<Int>()
        var fail = true
        val state = RelationFeedState { page ->
            calls += page
            if (page == 1) RelationPage(listOf(user(1), user(2)), 3, 2)
            else if (fail) { fail = false; error("offline") }
            else RelationPage(listOf(user(2), user(3)), 3, null)
        }
        state.fetch(); state.fetch()
        assertEquals(listOf(1L, 2L), state.items.map { it.mid })
        assertNotNull(state.error)
        state.retry()
        assertEquals(listOf(1, 2, 2), calls)
        assertEquals(listOf(1L, 2L, 3L), state.items.map { it.mid })
        assertNull(state.nextPage)
    }
    @Test fun failedRefreshKeepsRowsAndRetriesFirstPageInsteadOfContinuation() = runBlocking {
        var fail = false
        val calls = mutableListOf<Int>()
        val state = RelationFeedState { page ->
            calls += page
            if (fail) error("private")
            RelationPage(listOf(user(1)), 50, 2)
        }
        state.fetch(); fail = true; state.fetch(true)
        assertEquals(1, state.items.size)
        fail = false; state.retry()
        assertEquals(listOf(1, 1, 1), calls)
        assertNull(state.error)
    }
    @Test fun concurrentFooterCannotDuplicateRequestsAndCancellationReleasesBusyState() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val state = RelationFeedState { calls++; gate.await(); RelationPage(emptyList(), 0, null) }
        val request = launch(start = CoroutineStart.UNDISPATCHED) { state.fetch() }
        state.fetch()
        assertEquals(1, calls)
        request.cancelAndJoin()
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals(1, state.nextPage)
    }
    @Test fun repeatedPageCannotCauseInfiniteAutoPagination() = runBlocking {
        val state = RelationFeedState { RelationPage(listOf(user(1)), 100, it + 1) }
        state.fetch(); state.fetch()
        assertNull(state.nextPage)
        state.updateRelation(1, 6)
        assertTrue(state.items.single().followed)
    }
}
