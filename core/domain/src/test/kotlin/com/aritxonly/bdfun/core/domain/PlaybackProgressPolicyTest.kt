package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.domain.resumePlaybackProgress
import com.aritxonly.bdfun.core.domain.playbackProgressEvictions
import com.aritxonly.bdfun.core.domain.playbackProgressDecision
import org.junit.Assert.*
import org.junit.Test

class PlaybackProgressPolicyTest {
    @Test fun legacyCommaSeparatedValuesKeepPositionAndUnknownDuration() {
        assertEquals(12345L, resumePlaybackProgress("12345,100000,1700000000000"))
        assertEquals(12345L, resumePlaybackProgress("12345,invalid,1700000000000"))
        assertEquals(0L, resumePlaybackProgress("invalid,100000,1700000000000"))
    }
    @Test fun minimumAndEndBoundaryRemainInclusiveForSavedProgress() {
        assertEquals(ProgressSaveDecision.Skip, playbackProgressDecision(2999, 100000))
        assertEquals(ProgressSaveDecision.Write, playbackProgressDecision(3000, 100000))
        assertEquals(ProgressSaveDecision.Write, playbackProgressDecision(95000, 100000))
        assertEquals(ProgressSaveDecision.Clear, playbackProgressDecision(95001, 100000))
        assertEquals(0L, resumePlaybackProgress("95001,100000,1700000000000"))
    }
    @Test fun completedShortVideoRetainsOriginalSkipBeforeClearOrdering() {
        assertEquals(ProgressSaveDecision.Skip, playbackProgressDecision(2999, 4000))
        assertEquals(ProgressSaveDecision.Clear, playbackProgressDecision(3000, 4000))
    }
    @Test fun onlyExceedingFiveHundredTriggersTrimToLatestFourHundred() {
        val entries = (1..500).associate { "p_$it" to "5000,100000,$it" }
        assertTrue(playbackProgressEvictions(entries).isEmpty())
        val keys = playbackProgressEvictions(entries + ("p_501" to "5000,100000,501"))
        assertEquals(101, keys.size)
        assertEquals("p_1", keys.first()); assertEquals("p_101", keys.last())
    }
    @Test fun malformedTimestampIsEvictedBeforeRecentValidEntries() {
        val entries = (1..500).associate { "p_$it" to "5000,100000,$it" } + ("bad" to "broken")
        assertEquals("bad", playbackProgressEvictions(entries).first())
    }
}
