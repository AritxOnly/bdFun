package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class PlaybackProgressPolicyTest {
    @Test fun legacyCommaSeparatedValuesKeepPositionAndUnknownDuration() {
        assertEquals(12345L, PlaybackProgressPolicy.resume("12345,100000,1700000000000"))
        assertEquals(12345L, PlaybackProgressPolicy.resume("12345,invalid,1700000000000"))
        assertEquals(0L, PlaybackProgressPolicy.resume("invalid,100000,1700000000000"))
    }
    @Test fun minimumAndEndBoundaryRemainInclusiveForSavedProgress() {
        assertEquals(ProgressSaveDecision.Skip, PlaybackProgressPolicy.decision(2999, 100000))
        assertEquals(ProgressSaveDecision.Write, PlaybackProgressPolicy.decision(3000, 100000))
        assertEquals(ProgressSaveDecision.Write, PlaybackProgressPolicy.decision(95000, 100000))
        assertEquals(ProgressSaveDecision.Clear, PlaybackProgressPolicy.decision(95001, 100000))
        assertEquals(0L, PlaybackProgressPolicy.resume("95001,100000,1700000000000"))
    }
    @Test fun completedShortVideoRetainsOriginalSkipBeforeClearOrdering() {
        assertEquals(ProgressSaveDecision.Skip, PlaybackProgressPolicy.decision(2999, 4000))
        assertEquals(ProgressSaveDecision.Clear, PlaybackProgressPolicy.decision(3000, 4000))
    }
    @Test fun onlyExceedingFiveHundredTriggersTrimToLatestFourHundred() {
        val entries = (1..500).associate { "p_$it" to "5000,100000,$it" }
        assertTrue(PlaybackProgressPolicy.evictions(entries).isEmpty())
        val keys = PlaybackProgressPolicy.evictions(entries + ("p_501" to "5000,100000,501"))
        assertEquals(101, keys.size)
        assertEquals("p_1", keys.first()); assertEquals("p_101", keys.last())
    }
    @Test fun malformedTimestampIsEvictedBeforeRecentValidEntries() {
        val entries = (1..500).associate { "p_$it" to "5000,100000,$it" } + ("bad" to "broken")
        assertEquals("bad", PlaybackProgressPolicy.evictions(entries).first())
    }
}
