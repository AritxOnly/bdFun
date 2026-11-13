package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.feature.video.application.*
import org.junit.Assert.*
import org.junit.Test

class VideoPlayerAssemblyTest {
    private val video = VideoItem("one", 1, "列表标题", "列表封面", "列表UP", "列表头像", 7, 0, 0, 10, 0)
    private fun content() = VideoPlayerContent(video, null, null, null, emptyList(),
        VideoPlaybackSelection("one", 12, 0, resumePositionMs = 500), VideoInteractionState(), VideoMetadataState(), false)
    @Test fun loadingDetailKeepsOriginalListMetadataAndSeek() {
        val output = assembleVideoPlayer(content(), VideoPlayerFlags(false, true, false))
        assertEquals("列表封面", output.media.cover)
        assertEquals("列表标题", output.presentation.fullscreenInfo.title)
        assertEquals("自动", output.presentation.fullscreenInfo.qualityLabel)
        assertEquals(12L, output.presentation.fullscreenInfo.currentCid)
        assertEquals(500L, output.presentation.resumePositionMs)
        assertTrue(output.presentation.immersiveStatusBar)
    }
    @Test fun manualQualityAndLiveApplicationStateReachExistingControls() {
        val quality = Quality(64, "720P", 1280, 720, "30", "avc", "url")
        val output = assembleVideoPlayer(content().copy(quality = quality,
            selection = VideoPlaybackSelection("two", 99, 64),
            interaction = VideoInteractionState(liked = true, likeCount = 88),
            metadata = VideoMetadataState(onlineCount = 123, onlineText = "123"), loggedIn = true),
            VideoPlayerFlags(true, false, true))
        assertEquals("two", output.media.bvid)
        assertEquals("720P", output.presentation.fullscreenInfo.qualityLabel)
        assertTrue(output.presentation.fullscreenInfo.liked)
        assertEquals(88L, output.presentation.fullscreenInfo.likeCount)
        assertEquals(123L, output.presentation.fullscreenInfo.onlineCount)
        assertTrue(output.presentation.pictureInPicture)
    }
}
