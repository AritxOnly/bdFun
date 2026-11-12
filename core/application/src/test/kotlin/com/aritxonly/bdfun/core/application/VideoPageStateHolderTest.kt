package com.aritxonly.bdfun.core.application

import org.junit.Assert.*
import org.junit.Test

class VideoPageStateHolderTest {
    @Test fun twoPagesDoNotShareFullscreenReplyOrPlaybackProgress() {
        val first = VideoPageStateHolder()
        val second = VideoPageStateHolder()
        first.update { it.copy(fullscreen = true, replyRoot = 1, replyName = "UP", danmakuProgressMs = 9000) }
        assertFalse(second.state.value.fullscreen)
        assertNull(second.state.value.replyName)
        assertEquals(0L, second.state.value.danmakuProgressMs)
    }
    @Test fun oldSnapshotsRemainImmutable() {
        val holder = VideoPageStateHolder()
        val old = holder.state.value
        holder.update { it.copy(replyName = "UP", replyRoot = 1, replyParent = 2) }
        assertNull(old.replyName)
        assertEquals(2L, holder.state.value.replyParent)
    }
    @Test fun ownerPauseOnlyTargetsThatPagesPlayersAndDisablesAutoResume() {
        val registry = PlaybackPauseRegistry()
        val first = Any(); val second = Any()
        val calls = mutableListOf<Pair<Int, Boolean>>()
        registry.register(first) { calls += 1 to it }
        registry.register(second) { calls += 2 to it }
        registry.pauseFor(first)
        assertEquals(listOf(1 to false), calls)
    }
    @Test fun globalPauseReachesAllLivePlayersButNotUnregisteredOnes() {
        val registry = PlaybackPauseRegistry()
        val calls = mutableListOf<Int>()
        val first: (Boolean) -> Unit = { calls += 1 }
        val second: (Boolean) -> Unit = { calls += 2 }
        registry.register(Any(), first); registry.register(Any(), first)
        registry.register(Any(), second); registry.unregister(second)
        registry.pauseAll()
        assertEquals(listOf(1), calls)
    }
}
