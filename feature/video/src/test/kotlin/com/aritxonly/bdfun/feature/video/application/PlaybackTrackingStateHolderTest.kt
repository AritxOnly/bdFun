package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.application.PlaybackSnapshot
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackTrackingStateHolderTest {
    private class Fake : PlaybackHistoryRepository, SponsorRepository {
        var gate: CompletableDeferred<Unit>? = null
        var calls = 0
        var progress = 0L
        override suspend fun reportHistory(aid: Long, cid: Long, progressSec: Long): Outcome<Unit> {
            calls++; progress = progressSec; gate?.await(); return Outcome.Success(Unit)
        }
        override suspend fun segments(bvid: String): Outcome<List<SponsorSegment>> {
            gate?.await(); return Outcome.Success(listOf(SponsorSegment(0, 1000, "sponsor", "skip", bvid)))
        }
    }
    @Test fun slowHistoryCannotDuplicateReportOrBlockProgressPolling() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }
        val holder = PlaybackTrackingStateHolder(fake, fake)
        val tick = PlaybackSnapshot(playing = true, positionMs = 9999)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.reportIfDue(1, 2, tick, 15000) }
        holder.reportIfDue(1, 2, tick, 30000)
        assertEquals(1, fake.calls); assertEquals(9L, fake.progress)
        job.cancelAndJoin()
        assertFalse(holder.state.value.reporting)
    }
    @Test fun cancelledHistoryKeepsOriginalRetryInterval() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }
        val holder = PlaybackTrackingStateHolder(fake, fake)
        val tick = PlaybackSnapshot(playing = true, positionMs = 5000)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.reportIfDue(1, 2, tick, 15000) }
        job.cancelAndJoin(); fake.gate = null
        holder.reportIfDue(1, 2, tick, 16000)
        assertEquals(1, fake.calls)
        holder.reportIfDue(1, 2, tick, 30000)
        assertEquals(2, fake.calls)
    }
    @Test fun disablingSegmentsRejectsOlderResponse() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }
        val holder = PlaybackTrackingStateHolder(fake, fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.loadSegments("one", true) }
        holder.loadSegments("two", false)
        fake.gate!!.complete(Unit); job.join()
        assertEquals("two", holder.state.value.bvid)
        assertTrue(holder.state.value.segments.isEmpty())
    }
    @Test fun activeSegmentUsesMatchingSnapshotAndKeepsDismissalScopedToVideo() {
        val fake = Fake()
        val holder = PlaybackTrackingStateHolder(fake, fake)
        val segment = SponsorSegment(1000, 2000, "sponsor", "skip", "one")
        val snapshot = PlaybackTrackingState(bvid = "video-one", segments = listOf(segment))
        assertEquals(segment, holder.activeSegment(snapshot, "video-one", 1000, emptySet()))
        assertNull(holder.activeSegment(snapshot, "video-two", 1000, emptySet()))
        assertNull(holder.activeSegment(snapshot, "video-one", 1000, setOf("one")))
        assertEquals(segment, holder.activeSegment(snapshot, "video-one", 1000, emptySet()))
    }
}
