package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class BoundedVideoGridCellsTest {
    @Test fun unfoldedWindowFitsFourCards() {
        val cells = boundedVideoCellSizes(876, 10, 180, 260)
        assertEquals(4, cells.size)
        assertEquals(876, cells.sum() + 30)
    }
    @Test fun resizePreservesBoundsAndNeverOverflows() {
        for (width in 180..2500) {
            val cells = boundedVideoCellSizes(width, 10, 180, 260)
            assertTrue(cells.all { it in 180..260 })
            assertTrue(cells.sum() + (cells.size - 1) * 10 <= width)
        }
    }
    @Test fun tinyWindowUsesAvailableSpace() {
        assertEquals(listOf(120), boundedVideoCellSizes(120, 10, 180, 260))
    }
}
