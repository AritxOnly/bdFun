package com.aritxonly.bdfun

import com.aritxonly.bdfun.core.application.*
import com.aritxonly.bdfun.ui.DiscPlaybackState
import com.aritxonly.bdfun.ui.observeDiscPlayback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class DiscPlaybackProjectionTest {
    @Test fun progressBufferingAndOtherPageMetadataCannotInvalidateTheDisc() = runBlocking {
        val source = MutableStateFlow(PlaybackSessionState(cover = "cover", playback = PlaybackSnapshot(playing = true)))
        val seen = mutableListOf<DiscPlaybackState>()
        val observer = launch(Dispatchers.Unconfined) { observeDiscPlayback(source).toList(seen) }
        repeat(100) { frame -> source.value = source.value.copy(title = "title$frame", author = "$frame",
            fullscreen = frame % 2 == 0, playback = source.value.playback.copy(positionMs = frame * 500L,
                bufferedMs = frame * 1500L, ready = frame % 2 == 0, speed = 1f + frame)) }
        assertEquals(listOf(DiscPlaybackState("cover", true)), seen)
        source.value = source.value.copy(playback = source.value.playback.copy(playing = false))
        source.value = source.value.copy(cover = "next")
        assertEquals(listOf(DiscPlaybackState("cover", true), DiscPlaybackState("cover", false), DiscPlaybackState("next", false)), seen)
        observer.cancelAndJoin()
    }
    @Test fun eachCollectorGetsTheCurrentStateAndCancelStopsFurtherValues() = runBlocking {
        val source = MutableStateFlow(PlaybackSessionState(cover = "first"))
        val seen = mutableListOf<DiscPlaybackState>()
        val first = launch(Dispatchers.Unconfined) { observeDiscPlayback(source).toList(seen) }
        first.cancelAndJoin(); source.value = source.value.copy(cover = "second")
        assertEquals(listOf(DiscPlaybackState("first", false)), seen)
        val later = mutableListOf<DiscPlaybackState>()
        val second = launch(Dispatchers.Unconfined) { observeDiscPlayback(source).toList(later) }
        assertEquals(listOf(DiscPlaybackState("second", false)), later); second.cancelAndJoin()
    }
}
