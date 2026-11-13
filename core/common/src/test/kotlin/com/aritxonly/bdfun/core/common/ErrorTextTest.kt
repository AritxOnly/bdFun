package com.aritxonly.bdfun.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTextTest {
    @Test fun explicitFailureTextAndEmptyFallbackRemainVisible() {
        assertEquals("请先登录", AppError.LoginExpired.messageOr("fallback"))
        assertEquals("offline", AppError.Network("offline").messageOr("fallback"))
        assertEquals("fallback", AppError.Unknown("").messageOr("fallback"))
        assertEquals("risk", AppError.RiskControl("risk").messageOr("fallback"))
    }
}
