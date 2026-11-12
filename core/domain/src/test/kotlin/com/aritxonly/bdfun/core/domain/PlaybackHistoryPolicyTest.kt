package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class PlaybackHistoryPolicyTest {
    @Test fun firstThreeSecondsAndPausedStateDoNotReport() {
        assertFalse(PlaybackHistoryPolicy.shouldReport(true, false, 2999, 20000, 0))
        assertFalse(PlaybackHistoryPolicy.shouldReport(false, false, 9000, 20000, 0))
    }
    @Test fun normalPlaybackUsesFifteenSecondInterval() {
        assertFalse(PlaybackHistoryPolicy.shouldReport(true, false, 9000, 14999, 0))
        assertTrue(PlaybackHistoryPolicy.shouldReport(true, false, 9000, 15000, 0))
    }
    @Test fun endUsesThreeSecondSupplementInterval() {
        assertFalse(PlaybackHistoryPolicy.shouldReport(false, true, 9000, 2999, 0))
        assertTrue(PlaybackHistoryPolicy.shouldReport(false, true, 9000, 3000, 0))
    }
}
