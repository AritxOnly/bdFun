package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class RefreshRatePolicyTest {
    @Test fun touchAndDisabledSettingLeaveRateToSystem() {
        assertEquals(0f, RefreshRatePolicy.target(true, 1499, true, true))
        assertEquals(0f, RefreshRatePolicy.target(false, 9999, false, false))
    }
    @Test fun idleVideoAndPausedFullscreenKeepSixtyHz() {
        assertEquals(60f, RefreshRatePolicy.target(true, 1500, true, false))
        assertEquals(60f, RefreshRatePolicy.target(true, 1500, false, true))
    }
    @Test fun idleStaticPageKeepsOriginalOneHzRequest() {
        assertEquals(1f, RefreshRatePolicy.target(true, 1500, false, false))
    }
}
