package com.aritxonly.teak.common.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TeakContentColorsTest {
    @Test fun flatCanvasUsesAbsoluteBlackAndWhite() {
        assertEquals(Color.White, teakContentColors(false).background)
        assertEquals(Color.Black, teakContentColors(true).background)
    }

    @Test fun groupingRemainsVisibleInBothModes() {
        for (dark in listOf(false, true)) {
            val colors = teakContentColors(dark)
            assertNotEquals(colors.background, colors.groupedBackground)
        }
    }
}
