package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class BufferingDotsTest {
    @Test fun firstDotHasOriginalRampHoldFadeBoundaries() {
        for ((time, expected) in listOf(0f to 0f, 125f to .5f, 250f to 1f, 1399f to 1f, 1400f to 1f, 1525f to .5f, 1650f to 0f, 2699f to 0f)) {
            assertEquals("t=$time", expected, bufferingDot1(time), 0f)
        }
    }
    @Test fun secondDotStartsAndEnds250msAfterFirst() {
        for ((time, expected) in listOf(0f to 0f, 250f to 0f, 375f to .5f, 500f to 1f, 1650f to 1f, 1775f to .5f, 1900f to 0f, 2699f to 0f)) {
            assertEquals("t=$time", expected, bufferingDot2(time), 0f)
        }
    }
    @Test fun thirdDotRetainsOriginalDelayAndFade() {
        for ((time, expected) in listOf(0f to 0f, 500f to 0f, 625f to .5f, 750f to 1f, 1900f to 1f, 2025f to .5f, 2150f to 0f, 2699f to 0f)) {
            assertEquals("t=$time", expected, bufferingDot3(time), 0f)
        }
    }
}
