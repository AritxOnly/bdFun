package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class VideoExitOwnershipTest {
    @Test fun finishingOldActivityDoesNotPauseTheNewVideo() {
        val old = Any()
        val next = Any()
        val calls = mutableListOf<String>()
        val oldHook: (Boolean) -> Unit = { calls += "old:$it" }
        val newHook: (Boolean) -> Unit = { calls += "new:$it" }
        VideoPageBus.registerPause(old, oldHook)
        VideoPageBus.registerPause(next, newHook)
        try {
            VideoPageBus.pauseFor(old)
            assertEquals(listOf("old:false"), calls)
        } finally {
            VideoPageBus.unregisterPause(oldHook)
            VideoPageBus.unregisterPause(newHook)
        }
    }
    @Test fun dismissedPipCannotAutomaticallyResume() {
        val owner = Any()
        var allowResume = true
        val hook: (Boolean) -> Unit = { allowResume = it }
        VideoPageBus.registerPause(owner, hook)
        try {
            VideoPageBus.pauseFor(owner)
            assertFalse(allowResume)
        } finally { VideoPageBus.unregisterPause(hook) }
    }
    @Test fun destroyedPlayerIsNotCalledAgain() {
        val owner = Any()
        var calls = 0
        val hook: (Boolean) -> Unit = { calls++ }
        VideoPageBus.registerPause(owner, hook)
        VideoPageBus.unregisterPause(hook)
        VideoPageBus.pauseFor(owner)
        assertEquals(0, calls)
    }
}
