package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.PlayerCollapseMode
import org.junit.Assert.*
import org.junit.Test

class VideoCollapsePolicyTest {
    @Test fun onlyEnabledMatchingScrollPhaseConsumes() {
        assertNull(reduceVideoDetailPreScroll(0f, -10f, -100f, false, false))
        assertNull(reduceVideoDetailPreScroll(0f, -10f, -100f, true, true))
        assertNull(reduceVideoDetailPreScroll(-20f, 10f, -100f, true, false))
        assertNull(reduceVideoDetailPostScroll(-20f, -10f, -100f, true, false))
        assertNull(reduceVideoDetailPostScroll(-20f, 10f, -100f, true, false, layoutAlreadyCollapsed = true))
        assertEquals(VideoDetailScrollUpdate(-10f, 10f), reduceVideoDetailPostScroll(-20f, 10f, -100f, true, false))
    }
    @Test fun scrollClampsAndKeepsStrictMinimumDelta() {
        assertEquals(VideoDetailScrollUpdate(-50f, -40f), reduceVideoDetailPreScroll(-10f, -100f, -50f, true, false))
        assertNull(reduceVideoDetailPreScroll(-50f, -10f, -50f, true, false))
        assertNull(reduceVideoDetailPreScroll(0f, -0.74f, -50f, true, false))
        assertEquals(VideoDetailScrollUpdate(-0.75f, -0.75f), reduceVideoDetailPreScroll(0f, -0.75f, -50f, true, false))
    }
    @Test fun pausedIntroCommentsAndThreadOverrideOffMode() {
        for (tab in 0..1) assertTrue(shouldEnableInlinePortraitScrollTransform(PlayerCollapseMode.OFF, tab, false, true))
        assertFalse(shouldEnableInlinePortraitScrollTransform(PlayerCollapseMode.OFF, 0, false, false))
        assertTrue(shouldHidePausedInlinePlayerForComments(9, true, true))
        assertFalse(shouldHidePausedInlinePlayerForComments(9, true, false))
    }
    @Test fun modeOrientationAndUnknownValueKeepOriginalRules() {
        assertTrue(shouldEnableInlinePortraitScrollTransform(PlayerCollapseMode.INTRO_ONLY, 0, true))
        assertFalse(shouldEnableInlinePortraitScrollTransform(PlayerCollapseMode.INTRO_ONLY, 0, false))
        assertTrue(shouldEnableInlinePortraitScrollTransform(PlayerCollapseMode.COMMENT_ONLY, 1, false))
        assertFalse(shouldEnableInlinePortraitScrollTransform(PlayerCollapseMode.PAUSED_ONLY, 2, true, false))
        assertTrue(shouldEnableInlinePortraitScrollTransform(PlayerCollapseMode.PAUSED_ONLY, 2, true, true))
        for (mode in PlayerCollapseMode.entries) assertSame(mode, PlayerCollapseMode.fromValue(mode.value))
        assertSame(PlayerCollapseMode.OFF, PlayerCollapseMode.fromValue(99))
    }
    @Test fun pausedViewportOnlyRetainsToolbarWhenRequested() {
        assertEquals(56f, resolveInlinePlayerCollapsedViewportHeightDp(70f, true, true), 0f)
        assertEquals(0f, resolveInlinePlayerCollapsedViewportHeightDp(70f, true), 0f)
        assertEquals(0f, resolveInlinePlayerCollapsedViewportHeightDp(-5f, false), 0f)
    }
    @Test fun collapseResumeOnlyOwnsItsAutomaticPause() {
        assertTrue(resolvePlayerCollapsedForAutoPause(true, -99f, 100f, false))
        assertFalse(resolvePlayerCollapsedForAutoPause(true, -98f, 100f, false))
        assertFalse(resolvePlayerCollapsedForAutoPause(false, 0f, 0f, true, restoreRequested = true))
        assertTrue(shouldAutoPauseOnPlayerCollapse(true, true, true))
        assertFalse(shouldAutoPauseOnPlayerCollapse(true, true, true, true))
        assertFalse(shouldAutoResumeOnPlayerExpand(true, false, false))
        assertTrue(shouldAutoResumeOnPlayerExpand(true, false, true))
    }
}
