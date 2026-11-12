package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class VideoPlayerViewportTest {
    private fun input() = VideoViewportInput(200.dp, 56.dp, 128f, 24.dp, true, false, false, false)

    @Test fun immersiveTopUsesSameHalfCollapseProgress() {
        val output = resolveVideoPlayerViewport(input())
        assertEquals(0.5f, output.collapseProgress, 0.0001f)
        assertEquals(12.dp, output.playerTopInset)
        assertEquals(24.dp, resolveVideoPlayerViewport(input().copy(immersiveStatusBar = false)).playerTopInset)
    }
    @Test fun fullscreenPipAndSplitDisableCollapseWithoutAddingImmersiveInset() {
        for (value in listOf(input().copy(fullscreen = true), input().copy(pictureInPicture = true), input().copy(splitPanes = true))) {
            val output = resolveVideoPlayerViewport(value)
            assertEquals(0f, output.collapseProgress, 0f)
            assertEquals(0.dp, output.playerTopInset)
        }
    }
    @Test fun overshootAndDegenerateRangeKeepOriginalClamping() {
        assertEquals(0f, resolveVideoPlayerViewport(input().copy(animatedHeight = 250f)).collapseProgress, 0f)
        assertEquals(1f, resolveVideoPlayerViewport(input().copy(animatedHeight = 0f)).collapseProgress, 0f)
        assertEquals(0f, resolveVideoPlayerViewport(input().copy(expandedHeight = 50.dp)).collapseProgress, 0f)
    }
}
