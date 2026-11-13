package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class VideoExitOwnershipTest {
    private val pauses = com.aritxonly.bdfun.core.application.PlaybackPauseRegistry()
    @Test fun finishingOldActivityDoesNotPauseTheNewVideo() {
        val old = Any()
        val next = Any()
        val calls = mutableListOf<String>()
        val oldHook: (Boolean) -> Unit = { calls += "old:$it" }
        val newHook: (Boolean) -> Unit = { calls += "new:$it" }
        pauses.register(old, oldHook)
        pauses.register(next, newHook)
        try {
            pauses.pauseFor(old)
            assertEquals(listOf("old:false"), calls)
        } finally {
            pauses.unregister(oldHook)
            pauses.unregister(newHook)
        }
    }
    @Test fun dismissedPipCannotAutomaticallyResume() {
        val owner = Any()
        var allowResume = true
        val hook: (Boolean) -> Unit = { allowResume = it }
        pauses.register(owner, hook)
        try {
            pauses.pauseFor(owner)
            assertFalse(allowResume)
        } finally { pauses.unregister(hook) }
    }
    @Test fun destroyedPlayerIsNotCalledAgain() {
        val owner = Any()
        var calls = 0
        val hook: (Boolean) -> Unit = { calls++ }
        pauses.register(owner, hook)
        pauses.unregister(hook)
        pauses.pauseFor(owner)
        assertEquals(0, calls)
    }
}
