package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class SearchFilterStateTest {
    @Test fun sortIndicesKeepTheExistingProtocolValues() {
        assertEquals(listOf("totalrank", "pubdate", "click", "dm", "totalrank"),
            (0..4).map { SearchFilterState(order = it).orderParam })
    }
    @Test fun durationsRemainUnmodifiedIncludingTheExistingFifthOption() {
        assertEquals((0..5).toList(), (0..5).map { SearchFilterState(duration = it).durationParam })
    }
    @Test fun noPublicationLimitDoesNotReadTheClock() {
        assertNull(SearchFilterState().beginSecondsAt { error("无需时间") })
        assertNull(SearchFilterState(published = 4).beginSecondsAt { error("未知条件仍不限") })
    }
    @Test fun publicationWindowsRetainOneSevenAnd183Days() {
        val now = 2_000_000_000_999L
        assertEquals(listOf(1L, 7L, 183L).map { now / 1000 - it * 86400 },
            (1..3).map { SearchFilterState(published = it).beginSecondsAt { now } })
    }
    @Test fun qualityDoesNotChangeSortDurationOrTimeParameters() {
        val original = SearchFilterState(order = 2, published = 1, duration = 3)
        val preferred = original.copy(quality = 3)
        assertEquals(original.orderParam, preferred.orderParam)
        assertEquals(original.durationParam, preferred.durationParam)
        assertEquals(original.beginSecondsAt { 2_000_000_000_000 }, preferred.beginSecondsAt { 2_000_000_000_000 })
    }
}
