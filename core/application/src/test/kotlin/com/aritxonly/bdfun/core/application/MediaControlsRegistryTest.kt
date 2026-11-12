package com.aritxonly.bdfun.core.application

import org.junit.Assert.*
import org.junit.Test

class MediaControlsRegistryTest {
    private class Controls : SystemMediaControls {
        val commands = mutableListOf<String>()
        override fun toggle() { commands += "toggle" }
        override fun seekBy(deltaMs: Long) { commands += "seek:$deltaMs" }
        override fun previous() { commands += "previous" }
        override fun next() { commands += "next" }
    }
    @Test fun oldNotificationSessionCannotUnregisterCurrentSession() {
        val registry = MediaControlsRegistry()
        val old = Controls(); val current = Controls()
        registry.attach(old); registry.attach(current); registry.detach(old)
        registry.current!!.toggle(); registry.current!!.seekBy(10_000)
        assertTrue(old.commands.isEmpty())
        assertEquals(listOf("toggle", "seek:10000"), current.commands)
    }
    @Test fun releasedCurrentSessionNoLongerReceivesSystemCommands() {
        val registry = MediaControlsRegistry()
        val current = Controls()
        registry.attach(current); registry.detach(current)
        assertNull(registry.current)
    }
}
