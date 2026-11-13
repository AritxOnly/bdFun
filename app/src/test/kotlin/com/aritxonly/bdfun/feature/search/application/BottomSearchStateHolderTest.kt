package com.aritxonly.bdfun.feature.search.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BottomSearchStateHolderTest {
    private class Fake : SearchRepository {
        val calls = mutableListOf<String>()
        var request: SearchRequest? = null
        var people: suspend (String) -> Outcome<List<SearchUser>> = { Outcome.Success(emptyList()) }
        var result: Outcome<List<VideoItem>> = Outcome.Success(emptyList())
        override suspend fun hotKeywords(): Outcome<List<String>> { calls += "hot"; return Outcome.Success(listOf("热词")) }
        override suspend fun suggestions(query: String): Outcome<List<String>> { calls += query; return Outcome.Success(listOf(query)) }
        override suspend fun users(keyword: String): Outcome<List<SearchUser>> { calls += "users:$keyword"; return people(keyword) }
        override suspend fun videos(request: SearchRequest): Outcome<List<VideoItem>> {
            calls += "videos:${request.keyword}"; this.request = request; return result
        }
    }

    @Test fun waitsAndDefaultSearchOrderMatchMainAndRepeatedKeywordsReloadUsers() = runBlocking {
        val fake = Fake(); val waits = mutableListOf<Long>()
        val holder = BottomSearchStateHolder(fake) { waits += it }
        holder.search("word"); holder.search(null); holder.search("word")
        assertEquals(listOf("users:word", "videos:word", "users:word", "videos:word"), fake.calls)
        assertEquals(listOf(90L, 90L), waits)
        assertEquals(SearchRequest("word", "totalrank", 0, null, null), fake.request)
        assertTrue(holder.state.value.loadedOnce); assertFalse(holder.state.value.loading)
    }

    @Test fun debounceClearsOnSubmissionAndHotRequestRespectsSetting() = runBlocking {
        val fake = Fake(); val waits = mutableListOf<Long>()
        val holder = BottomSearchStateHolder(fake) { waits += it }
        holder.loadHotKeywords(false); assertTrue(fake.calls.isEmpty())
        holder.loadHotKeywords(true); holder.suggest(" word ", null)
        assertEquals(listOf("word"), holder.state.value.suggestions)
        holder.suggest("word", "word")
        assertTrue(holder.state.value.suggestions.isEmpty())
        assertEquals(listOf(180L), waits); assertEquals(listOf("hot", "word"), fake.calls)
    }

    @Test fun oldUserResultCannotPublishOrStartVideosAfterLandingReset() = runBlocking {
        val fake = Fake(); val old = CompletableDeferred<Outcome<List<SearchUser>>>()
        fake.people = { old.await() }
        val holder = BottomSearchStateHolder(fake) {}
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.search("old") }
        holder.search(null); old.complete(Outcome.Success(listOf(SearchUser(1, "old", "", "")))); job.join()
        assertTrue(holder.state.value.users.isEmpty()); assertEquals(listOf("users:old"), fake.calls)
        assertFalse(holder.state.value.loading)
    }

    @Test fun cancellationReleasesLoadingAndDoesNotRequestVideos() = runBlocking {
        val fake = Fake(); fake.people = { awaitCancellation() }
        val holder = BottomSearchStateHolder(fake) {}
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.search("cancel") }
        job.cancelAndJoin()
        assertFalse(holder.state.value.loading); assertFalse(holder.state.value.loadedOnce)
        assertEquals(listOf("users:cancel"), fake.calls)
    }

    @Test fun videoFailureRetainsServerMessageAndCompletesLoading() = runBlocking {
        val fake = Fake(); fake.result = Outcome.Failure(AppError.Unknown("原错误"))
        val holder = BottomSearchStateHolder(fake) {}
        holder.search("word")
        assertEquals("原错误", holder.state.value.error)
        assertTrue(holder.state.value.loadedOnce); assertFalse(holder.state.value.loading)
    }
}
