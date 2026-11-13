package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.Quality
import org.junit.Assert.*
import org.junit.Test

class AutomaticVideoQualitySelectorTest {
    private fun quality(id: Int, url: String = "$id") = Quality(id, "", 0, 0, "", "", url)
    private fun select(kbps: Int, ids: List<Int>) =
        AutomaticVideoQualitySelector { kbps }.select(ids.map { quality(it) })?.id
    private val ladder = listOf(127, 120, 116, 112, 80, 74, 64, 32, 16)

    @Test fun emptyListDoesNotReadPlatformBandwidth() {
        assertNull(AutomaticVideoQualitySelector { error("must not read") }.select(emptyList()))
    }
    @Test fun exactThresholdsUse1024Conversion() {
        val thresholds = listOf(40960 to 127, 24576 to 120, 10240 to 112,
            6144 to 80, 3072 to 64, 1536 to 32, 1 to 16, 0 to 64, -1 to 64)
        for ((kbps, expected) in thresholds) assertEquals("Kbps=$kbps", expected, select(kbps, ladder))
    }
    @Test fun valuesImmediatelyBelowThresholdUsePreviousTier() {
        for ((kbps, expected) in listOf(40959 to 120, 24575 to 112, 10239 to 80,
            6143 to 64, 3071 to 32, 1535 to 16)) assertEquals(expected, select(kbps, ladder))
    }
    @Test fun usesLadderOrderRatherThanInputOrderAndAllows116And74Fallback() {
        assertEquals(116, select(24576, listOf(16, 125, 116, 80, 126)))
        assertEquals(74, select(6144, listOf(16, 74, 64)))
    }
    @Test fun noHigherTierIsSelectedWhenLowerLadderCandidatesExist() {
        assertEquals(16, select(1, listOf(127, 80, 16)))
        assertEquals(32, select(3072, listOf(127, 80, 32)))
    }
    @Test fun absentLadderCandidateKeepsOriginalMinIdFallbackEvenForHdr() {
        assertEquals(125, select(0, listOf(126, 125)))
        assertEquals(127, select(1, listOf(127)))
        assertEquals(-1, select(40960, listOf(126, -1)))
    }
    @Test fun duplicateTierUsesFirstOriginalItem() {
        val first = quality(80, "first")
        assertSame(first, AutomaticVideoQualitySelector { 6144 }.select(listOf(first, quality(80, "second"))))
    }
    @Test fun readsCurrentBandwidthOnEachSelection() {
        var kbps = 1
        val selector = AutomaticVideoQualitySelector { kbps }
        val items = ladder.map { quality(it) }
        assertEquals(16, selector.select(items)?.id)
        kbps = 40960
        assertEquals(127, selector.select(items)?.id)
    }
}
