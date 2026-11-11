package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class SpaceHeaderGeometryTest {
    @Test fun freshCompactLayoutDoesNotReuseWideTabPosition() {
        assertFalse(spaceTabOffset(0, null, 80f).isFinite())
        assertEquals(600f, spaceTabOffset(0, 600, 80f), 0f)
    }
    @Test fun markerStopsAtHeaderAndRemainsPinnedWhenScrolledOffscreen() {
        assertEquals(80f, spaceTabOffset(0, 75, 80f), 0f)
        assertEquals(80f, spaceTabOffset(2, null, 80f), 0f)
        // Recomputed density/status-bar inset is authoritative after rotation.
        assertEquals(112f, spaceTabOffset(2, null, 112f), 0f)
    }
}
