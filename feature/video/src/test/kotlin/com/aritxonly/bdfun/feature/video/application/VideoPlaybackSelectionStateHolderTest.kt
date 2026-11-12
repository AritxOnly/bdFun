package com.aritxonly.bdfun.feature.video.application

import org.junit.Assert.*
import org.junit.Test

class VideoPlaybackSelectionStateHolderTest {
    @Test fun cloudCannotOverwriteExplicitOrLocalPosition() {
        val holder = VideoPlaybackSelectionStateHolder("one", 1, 80, 123L)
        holder.offerCloudResume(500L)
        assertEquals(123L, holder.state.value.resumePositionMs)
    }
    @Test fun manualQualityKeepsPositionAndAutomaticDoesNotRequireStreamReload() {
        val holder = VideoPlaybackSelectionStateHolder("one", 1, 80, 0)
        assertTrue(holder.selectQuality(64, 700L))
        assertFalse(holder.state.value.autoQuality)
        assertEquals(700L, holder.state.value.resumePositionMs)
        assertTrue(holder.selectQuality(0, 800L))
        assertTrue(holder.state.value.autoQuality)
        assertFalse(holder.selectQuality(0, 900L))
        assertEquals(800L, holder.state.value.resumePositionMs)
    }
    @Test fun newEpisodeResetsPositionButKeepsQuality() {
        val holder = VideoPlaybackSelectionStateHolder("one", 1, 64, 500)
        holder.selectPage(2, "two")
        assertEquals("two", holder.state.value.bvid)
        assertEquals(2L, holder.state.value.cid)
        assertEquals(0L, holder.state.value.resumePositionMs)
        assertEquals(64, holder.state.value.quality)
    }
    @Test fun restoringCidDoesNotDiscardExplicitSeek() {
        val holder = VideoPlaybackSelectionStateHolder("one", 1, 80, 100)
        holder.restoreCid(2)
        assertEquals(100L, holder.state.value.resumePositionMs)
    }
}
