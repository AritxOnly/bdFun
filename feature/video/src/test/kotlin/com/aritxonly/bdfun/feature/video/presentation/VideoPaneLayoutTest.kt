package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class VideoPaneLayoutTest {
    @Test fun phoneLandscapeKeepsReadableComments() {
        val layout = resolveVideoPaneLayout(844f, 390f)
        assertTrue(layout.isSplit)
        assertEquals(300f, layout.detailsWidthDp, 0.01f)
        assertEquals(544f, layout.playerWidthDp, 0.01f)
    }

    @Test fun largeLandscapeUsesSeventyThirty() {
        val layout = resolveVideoPaneLayout(1280f, 800f)
        assertEquals(384f, layout.detailsWidthDp, 0.01f)
        assertEquals(896f, layout.playerWidthDp, 0.01f)
    }

    @Test fun compactAndPortraitWindowsKeepStackedLayout() {
        assertFalse(resolveVideoPaneLayout(599f, 390f).isSplit)
        assertFalse(resolveVideoPaneLayout(800f, 1280f).isSplit)
        assertFalse(resolveVideoPaneLayout(800f, 800f).isSplit)
        assertFalse(resolveVideoPaneLayout(800f, 0f).isSplit)
    }

    @Test fun splitBoundaryReservesBothPanesWithoutOverflow() {
        for (width in listOf(600f, 720f, 999f, 1000f, 1920f)) {
            val layout = resolveVideoPaneLayout(width, 390f)
            assertTrue(layout.detailsWidthDp >= 300f)
            assertTrue(layout.playerWidthDp >= 300f)
            assertEquals(width, layout.playerWidthDp + layout.detailsWidthDp, 0.01f)
        }
    }
}
