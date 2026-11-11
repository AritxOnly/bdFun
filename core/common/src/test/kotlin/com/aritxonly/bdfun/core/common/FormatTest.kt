package com.aritxonly.bdfun.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class FormatTest {
    @Test fun countBoundaries() = withDefaults {
        assertEquals("0", formatCount(0))
        assertEquals("-1", formatCount(-1))
        assertEquals("9999", formatCount(9999))
        assertEquals("1.0 万", formatCount(10000))
        assertEquals("1.0 亿", formatCount(100000000))
        assertEquals("92233720368.5 亿", formatCount(Long.MAX_VALUE))
    }

    @Test fun durationBoundaries() = withDefaults {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:-1", formatDuration(-1))
        assertEquals("59:59", formatDuration(3599))
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("596523:14:07", formatDuration(Int.MAX_VALUE))
    }

    @Test fun datesAcrossYearBoundary() = withDefaults {
        assertEquals("1970-01-01", formatDate(0))
        assertEquals("1969-12-31", formatDate(-1))
        assertEquals("2023-12-31", formatDate(1704067199))
        assertEquals("2024-01-01", formatDate(1704067200))
        assertEquals("1970-01-01 00:00", formatDateTime(0))
        assertEquals("1969-12-31 23:59", formatDateTime(-1))
        assertEquals("2023-12-31 23:59", formatDateTime(1704067199))
        assertEquals("2024-01-01 00:00", formatDateTime(1704067200))
        assertEquals("2100-01-01", formatDate(4102444800))
        assertEquals("2100-01-01 00:00", formatDateTime(4102444800))
    }

    private fun withDefaults(block: () -> Unit) {
        val locale = Locale.getDefault()
        val zone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.CHINA)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            block()
        } finally {
            Locale.setDefault(locale)
            TimeZone.setDefault(zone)
        }
    }
}
