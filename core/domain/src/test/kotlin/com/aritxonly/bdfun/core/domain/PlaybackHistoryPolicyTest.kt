package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.domain.shouldReportPlaybackHistory
import org.junit.Assert.*
import org.junit.Test

class PlaybackHistoryPolicyTest {
    @Test fun firstThreeSecondsAndPausedStateDoNotReport() {
        assertFalse(shouldReportPlaybackHistory(true, false, 2999, 20000, 0))
        assertFalse(shouldReportPlaybackHistory(false, false, 9000, 20000, 0))
    }
    @Test fun normalPlaybackUsesFifteenSecondInterval() {
        assertFalse(shouldReportPlaybackHistory(true, false, 9000, 14999, 0))
        assertTrue(shouldReportPlaybackHistory(true, false, 9000, 15000, 0))
    }
    @Test fun endUsesThreeSecondSupplementInterval() {
        assertFalse(shouldReportPlaybackHistory(false, true, 9000, 2999, 0))
        assertTrue(shouldReportPlaybackHistory(false, true, 9000, 3000, 0))
    }
}
