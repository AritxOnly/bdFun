package com.aritxonly.bdfun.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudResumePolicyTest {
    @Test fun anotherPageNeverRestoresItsProgress() {
        assertEquals(0L, resolveCloudResumePositionMs(2, 1, 10, 99))
        assertEquals(10000L, resolveCloudResumePositionMs(1, 1, 10, 99))
        assertEquals(10000L, resolveCloudResumePositionMs(0, 1, 10, 99))
    }
    @Test fun absentAndNegativeProgressRemainZero() {
        assertEquals(0L, resolveCloudResumePositionMs(1, 1, 0, 99))
        assertEquals(0L, resolveCloudResumePositionMs(1, 1, -1, 99))
    }
    @Test fun inclusiveTwiceDurationThresholdKeepsSecondsAndMillisecondsDistinct() {
        assertEquals(198000L, resolveCloudResumePositionMs(1, 1, 198, 99))
        assertEquals(199L, resolveCloudResumePositionMs(1, 1, 199, 99))
        assertEquals(20000L, resolveCloudResumePositionMs(1, 1, 20000, 99))
    }
    @Test fun unknownDurationDoesNotScalePositiveProgress() {
        assertEquals(10L, resolveCloudResumePositionMs(0, 1, 10, 0))
        assertEquals(10L, resolveCloudResumePositionMs(0, 1, 10, -1))
    }
}
