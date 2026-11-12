package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.navigation.Route
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class IncomingVideoLinkStateHolderTest {
    private val first = "BV1orW46aEba"
    private val second = "BV1xx411c7mD"
    private class Fake : VideoLinkResolver {
        val requests = mutableListOf<String>()
        var result: VideoLinkResolution = VideoLinkResolution.Unresolved
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun resolve(text: String): VideoLinkResolution {
            requests += text; gate?.await(); return result
        }
    }
    @Test fun blankAndUnsupportedIntentsDoNotUpdateLastHandled() {
        val holder = IncomingVideoLinkStateHolder(Fake())
        for (text in listOf("", " \n ", "av123", "hello")) assertNull(holder.begin(text))
        assertEquals(IncomingVideoLinkState(), holder.state.value)
    }
    @Test fun clipboardRecordsUnsupportedTextButNotBlank() {
        val holder = IncomingVideoLinkStateHolder(Fake())
        assertNull(holder.clipboard("hello")); assertEquals("hello", holder.state.value.lastClipboard)
        assertNull(holder.clipboard(" \n ")); assertEquals("hello", holder.state.value.lastClipboard)
        assertEquals("", holder.state.value.lastHandledLink)
    }
    @Test fun acceptedRequestIsMarkedBeforeResolutionAndAdjacentDuplicateIsIgnored() {
        val holder = IncomingVideoLinkStateHolder(Fake())
        assertNotNull(holder.begin(first)); assertEquals(first, holder.state.value.lastHandledLink)
        assertNull(holder.begin(first))
    }
    @Test fun deduplicationUsesRawTextRatherThanNormalizedTarget() {
        val holder = IncomingVideoLinkStateHolder(Fake())
        for (text in listOf(first, "https://www.bilibili.com/video/$first", "prefix $first suffix", first)) {
            assertNotNull(text, holder.begin(text))
        }
    }
    @Test fun unresolvedRequestStaysHandledAndDoesNotRetrySameRawText() = runBlocking {
        val fake = Fake(); val holder = IncomingVideoLinkStateHolder(fake)
        assertNull(holder.resolve(holder.begin(first)!!)); assertNull(holder.begin(first))
        assertEquals(listOf(first), fake.requests)
    }
    @Test fun entryRecognitionKeepsUnsupportedAvUrlAsSilentResolutionAttempt() = runBlocking {
        val fake = Fake(); val holder = IncomingVideoLinkStateHolder(fake)
        val url = "https://www.bilibili.com/video/av123"
        assertNull(holder.begin("av123")); assertNull(holder.resolve(holder.begin(url)!!))
        assertEquals(listOf(url), fake.requests)
    }
    @Test fun emitsOriginalDefaultVideoRouteWithoutAddingTimestampOrCommentParsing() = runBlocking {
        val fake = Fake().apply { result = VideoLinkResolution.Resolved(first) }
        val holder = IncomingVideoLinkStateHolder(fake)
        assertEquals(Route.Video(bvid = first), holder.resolve(holder.begin("https://b23.tv/test")!!))
    }
    @Test fun differentRequestsRemainParallelAndNavigateInCompletionOrder() = runBlocking {
        val a = CompletableDeferred<Unit>(); val b = CompletableDeferred<Unit>()
        val resolver = VideoLinkResolver { text ->
            if (text == first) a.await() else b.await()
            VideoLinkResolution.Resolved(text)
        }
        val holder = IncomingVideoLinkStateHolder(resolver)
        val old = async(start = CoroutineStart.UNDISPATCHED) { holder.resolve(holder.begin(first)!!) }
        val latest = async(start = CoroutineStart.UNDISPATCHED) { holder.resolve(holder.begin(second)!!) }
        b.complete(Unit); assertEquals(Route.Video(second), latest.await()); assertFalse(old.isCompleted)
        a.complete(Unit); assertEquals(Route.Video(first), old.await())
    }
    @Test fun requestCannotResolveTwice() = runBlocking {
        val fake = Fake().apply { result = VideoLinkResolution.Resolved(first) }
        val holder = IncomingVideoLinkStateHolder(fake); val request = holder.begin(first)!!
        assertNotNull(holder.resolve(request)); assertNull(holder.resolve(request)); assertEquals(1, fake.requests.size)
    }
    @Test fun foreignHolderCannotConsumeRequestOrPreventItsOwnerResolving() = runBlocking {
        val fake = Fake().apply { result = VideoLinkResolution.Resolved(first) }
        val holder = IncomingVideoLinkStateHolder(fake); val other = IncomingVideoLinkStateHolder(fake)
        val request = holder.begin(first)!!
        assertNull(other.resolve(request)); assertTrue(fake.requests.isEmpty())
        assertEquals(Route.Video(first), holder.resolve(request))
    }
    @Test fun closeRejectsNewInputWithoutMutatingSnapshot() {
        val holder = IncomingVideoLinkStateHolder(Fake()); holder.begin(first)
        val snapshot = holder.state.value; holder.close(); holder.close()
        assertNull(holder.begin(second)); assertNull(holder.clipboard(second)); assertEquals(snapshot, holder.state.value)
    }
    @Test fun closeBeforeDispatchDoesNotCallResolver() = runBlocking {
        val fake = Fake(); val holder = IncomingVideoLinkStateHolder(fake); val request = holder.begin(first)!!
        holder.close(); assertNull(holder.resolve(request)); assertTrue(fake.requests.isEmpty())
    }
    @Test fun closeDuringResolutionSuppressesLateNavigation() = runBlocking {
        val fake = Fake().apply { result = VideoLinkResolution.Resolved(first); gate = CompletableDeferred() }
        val holder = IncomingVideoLinkStateHolder(fake)
        val request = async(start = CoroutineStart.UNDISPATCHED) { holder.resolve(holder.begin(first)!!) }
        holder.close(); fake.gate!!.complete(Unit); assertNull(request.await())
    }
    @Test fun cancellationPropagatesAndDoesNotResetDeduplication() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }; val holder = IncomingVideoLinkStateHolder(fake)
        val pending = holder.begin(first)!!
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.resolve(pending) }
        job.cancelAndJoin(); assertTrue(job.isCancelled); assertNull(holder.begin(first)); assertNull(holder.resolve(pending))
    }
    @Test fun nonCooperativeResolverCannotNavigateAfterCallerCancellation() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val holder = IncomingVideoLinkStateHolder(VideoLinkResolver {
            withContext(NonCancellable) { gate.await() }; VideoLinkResolution.Resolved(first)
        })
        var navigation: Route.Video? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) { navigation = holder.resolve(holder.begin(first)!!) }
        job.cancel(); gate.complete(Unit); job.join(); assertNull(navigation)
    }
    @Test fun clipboardAndIntentKeepIndependentRecentTextRules() {
        val holder = IncomingVideoLinkStateHolder(Fake())
        assertNotNull(holder.clipboard(first)); assertNotNull(holder.begin(second))
        assertNull(holder.clipboard(first)); assertEquals(second, holder.state.value.lastHandledLink)
        assertNotNull(holder.begin(first))
        assertEquals(first, holder.state.value.lastClipboard)
    }
    @Test fun recreatedActivityStartsWithFreshDeduplication() {
        val fake = Fake(); val holder = IncomingVideoLinkStateHolder(fake)
        assertNotNull(holder.clipboard(first)); holder.close()
        assertNotNull(IncomingVideoLinkStateHolder(fake).clipboard(first))
    }
}
