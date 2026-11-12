package com.aritxonly.bdfun.core.application

import org.junit.Assert.*
import org.junit.Test

class InlinePlaybackAspectTest {
    @Test fun validVideoAndPixelAspectReplaceTheOriginalRatio() {
        assertEquals(16f / 9f, inlinePlaybackAspect(1920, 1080, 1f, 1f), 0.00001f)
        assertEquals(2f, inlinePlaybackAspect(1600, 1000, 1.25f, 1f), 0.00001f)
    }
    @Test fun zeroOrNegativeDimensionsKeepCurrentLayout() {
        for ((w, h) in listOf(0 to 1080, 1920 to 0, -1 to 1, 1 to -1)) {
            assertEquals(1.5f, inlinePlaybackAspect(w, h, 1f, 1.5f), 0f)
        }
    }
    @Test fun nonFiniteAndStrictSmallRatioBoundaryKeepCurrentLayout() {
        for (ratio in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f, 0.05f)) {
            assertEquals(1.5f, inlinePlaybackAspect(1, 1, ratio, 1.5f), 0f)
        }
        assertEquals(0.0501f, inlinePlaybackAspect(1, 1, 0.0501f, 1.5f), 0f)
    }
}
