package com.aritxonly.bdfun.feature.sent.application

import com.aritxonly.bdfun.core.model.user.SentRecord
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class SentHistoryStateHolderTest {
    private fun record(id: Long, kind: String) = SentRecord(id, kind, "text$id", "BV1234567890", "title", 4567, 0)
    @Test fun snapshotPreservesOrderAndDoesNotObserveLaterSourceMutations() {
        val source = mutableListOf(record(2, "comment"), record(1, "danmaku"))
        val holder = SentHistoryStateHolder(source); source.clear()
        assertEquals(listOf(2L, 1L), holder.state.value.records.map { it.id })
    }
    @Test fun filtersAndCountsUseExactKindsAndUnknownFilterShowsAll() {
        val records = listOf(record(1, "comment"), record(2, "danmaku"), record(3, "Comment"), record(4, "other"))
        val holder = SentHistoryStateHolder(records)
        assertEquals(listOf(4, 1, 1), (0..2).map { holder.state.value.count(it) })
        holder.select(1); assertEquals(listOf(records[0]), holder.state.value.shown)
        holder.select(2); assertEquals(listOf(records[1]), holder.state.value.shown)
        holder.select(3); assertEquals(records, holder.state.value.shown)
    }
    @Test fun videoTargetRetainsProgressAndOriginalEmptyDefaults() {
        val holder = SentHistoryStateHolder(emptyList()); val record = record(1, "danmaku")
        assertNull(holder.videoFor(record.copy(bvid = " \n")))
        val video = holder.videoFor(record)!!
        assertEquals(record.bvid, video.bvid); assertEquals(record.title, video.title); assertEquals(4567L, video.startPositionMs)
        assertEquals(0L, video.aid); assertEquals(0L, video.cid); assertEquals("", video.cover); assertEquals(0, video.duration)
    }
    @Test fun timeFormattingKeepsNegativeClampHoursAndCurrentLocaleTimezone() {
        val locale = Locale.getDefault(); val zone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.US); TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("00:00", formatVideoTime(-1)); assertEquals("01:01", formatVideoTime(61_999))
            assertEquals("1:01:01", formatVideoTime(3_661_000)); assertEquals("1970-01-01 00:00", formatSentAt(0))
        } finally { Locale.setDefault(locale); TimeZone.setDefault(zone) }
    }
}
