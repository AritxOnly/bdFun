package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.SponsorSegment
import org.junit.Assert.*
import org.junit.Test

class SponsorSegmentPolicyTest {
    private fun segment(id: String, start: Long, end: Long) = SponsorSegment(start, end, "sponsor", "skip", id)

    @Test fun startIsIncludedAndEndIsExcluded() {
        val segment = segment("one", 1000, 2000)
        assertNull(SponsorSegmentPolicy.activeAt(listOf(segment), 999, emptySet()))
        assertEquals(segment, SponsorSegmentPolicy.activeAt(listOf(segment), 1000, emptySet()))
        assertEquals(segment, SponsorSegmentPolicy.activeAt(listOf(segment), 1999, emptySet()))
        assertNull(SponsorSegmentPolicy.activeAt(listOf(segment), 2000, emptySet()))
    }

    @Test fun overlappingSegmentsKeepOriginalOrderAndDismissalRevealsNext() {
        val first = segment("first", 1000, 3000)
        val second = segment("second", 0, 4000)
        assertEquals(first, SponsorSegmentPolicy.activeAt(listOf(first, second), 1500, emptySet()))
        assertEquals(second, SponsorSegmentPolicy.activeAt(listOf(first, second), 1500, setOf("first")))
        assertNull(SponsorSegmentPolicy.activeAt(listOf(first, second), 1500, setOf("first", "second")))
    }

    @Test fun zeroLengthReversedAndEmptySegmentsDoNotActivate() {
        assertNull(SponsorSegmentPolicy.activeAt(listOf(segment("zero", 1000, 1000)), 1000, emptySet()))
        assertNull(SponsorSegmentPolicy.activeAt(listOf(segment("backward", 2000, 1000)), 1500, emptySet()))
        assertNull(SponsorSegmentPolicy.activeAt(emptyList(), 0, emptySet()))
    }
}
