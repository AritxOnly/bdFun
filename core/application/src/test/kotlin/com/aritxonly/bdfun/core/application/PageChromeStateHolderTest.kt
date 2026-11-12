package com.aritxonly.bdfun.core.application

import org.junit.Assert.*
import org.junit.Test

class PageChromeStateHolderTest {
    @Test fun overlappingPagesDoNotShareBatchSelectionOrMessageCommands() {
        val first = PageChromeStateHolder(); val second = PageChromeStateHolder()
        first.update { it.copy(batchVisible = true, batchCount = 3, readAllRequest = 1) }
        assertFalse(second.state.value.batchVisible)
        assertEquals(0, second.state.value.batchCount)
        assertEquals(0, second.state.value.readAllRequest)
    }
    @Test fun changingScrollRegistrationDoesNotMutatePreviouslyObservedState() {
        val holder = PageChromeStateHolder()
        holder.update { it.copy(page = "home", scrolled = true) }
        val home = holder.state.value
        holder.update { it.copy(page = "dynamic", scrolled = false) }
        assertEquals("home", home.page)
        assertTrue(home.scrolled)
        assertEquals("dynamic", holder.state.value.page)
    }
}
