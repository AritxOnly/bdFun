package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.VideoItem
import org.junit.Assert.*
import org.junit.Test

class PlaybackQueueStateHolderTest {
    private fun video(id: String) = VideoItem(id, 1, id, "", "", "", 0, 0, 0, 0, 0)
    @Test fun queueKeepsOrderAndDoesNotWrapAtEnd() {
        val queue = PlaybackQueueStateHolder()
        queue.set(listOf(video("a"), video("b"), video("c")), "a")
        assertEquals("b", queue.next()?.bvid)
        assertEquals("c", queue.next()?.bvid)
        assertNull(queue.next())
    }
    @Test fun missingAnchorCannotStartQueueFromWrongItem() {
        val queue = PlaybackQueueStateHolder()
        queue.set(listOf(video("a")), "missing")
        assertFalse(queue.hasNext())
        assertNull(queue.next())
    }
    @Test fun publishedQueueIsIndependentOfMutableCallerList() {
        val queue = PlaybackQueueStateHolder()
        val items = mutableListOf(video("a"), video("b"))
        queue.set(items, "a"); items.clear()
        assertEquals("b", queue.next()?.bvid)
        queue.clear()
        assertFalse(queue.contains("a"))
        assertFalse(queue.hasNext())
    }
}
