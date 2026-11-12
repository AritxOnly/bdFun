package com.aritxonly.teak.common.chrome

import org.junit.Assert.assertEquals
import org.junit.Test

class TeakContentWidthTest {
    @Test fun narrowAndWideKeepOriginalWidthAndCenteringPolicy() {
        assertEquals(599, resolveTeakContentWidth(599, null, 600, 680))
        assertEquals(420, resolveTeakContentWidth(600, null, 600, 680))
        assertEquals(680, resolveTeakContentWidth(1200, null, 600, 680))
    }
    @Test fun explicitAndUnboundedWidthsKeepOriginalLimits() {
        assertEquals(500, resolveTeakContentWidth(500, 680, 600, 680))
        assertEquals(1000, resolveTeakContentWidth(1400, 1000, 600, 680))
        assertEquals(680, resolveTeakContentWidth(null, null, 600, 680))
        assertEquals(1000, resolveTeakContentWidth(null, 1000, 600, 680))
    }
}
