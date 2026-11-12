package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.domain.targetPlaybackRefreshRate
import org.junit.Assert.*
import org.junit.Test

class RefreshRatePolicyTest {
    @Test fun touchAndDisabledSettingLeaveRateToSystem() {
        assertEquals(0f, targetPlaybackRefreshRate(true, 1499, true, true))
        assertEquals(0f, targetPlaybackRefreshRate(false, 9999, false, false))
    }
    @Test fun idleVideoAndPausedFullscreenKeepSixtyHz() {
        assertEquals(60f, targetPlaybackRefreshRate(true, 1500, true, false))
        assertEquals(60f, targetPlaybackRefreshRate(true, 1500, false, true))
    }
    @Test fun idleStaticPageKeepsOriginalOneHzRequest() {
        assertEquals(1f, targetPlaybackRefreshRate(true, 1500, false, false))
    }
}
