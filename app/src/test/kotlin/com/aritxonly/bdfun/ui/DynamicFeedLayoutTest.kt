package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class DynamicFeedLayoutTest {
    @Test fun narrowWindowsRemainOneColumn() {
        for (width in listOf(320f, 360f, 600f, 735f)) assertEquals(1, dynamicFeedColumns(width))
    }
    @Test fun twoColumnsHaveReadableCardWidthsAndStayBounded() {
        for (width in listOf(736f, 840f, 1280f, 1920f)) {
            assertEquals(2, dynamicFeedColumns(width))
            val cardWidth = (minOf(width, 1320f) - 16f - 12f) / 2
            assertTrue(cardWidth >= 354f)
            assertTrue(cardWidth <= 646f)
        }
    }
}
