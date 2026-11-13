package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class VideoCollectionScrollTargetTest {
    @Test fun largeCollectionTargetsUseItemIndices() {
        assertEquals(0, videoCollectionScrollIndex(0, 138, 277))
        assertEquals(276, videoCollectionScrollIndex(2, 138, 277))
        assertEquals(138, videoCollectionScrollIndex(1, 138, 277))
        assertEquals(138, videoCollectionScrollIndex(null, 138, 277))
    }
    @Test fun missingCurrentEpisodeAndShrinkingCollectionsStayInBounds() {
        assertEquals(0, videoCollectionScrollIndex(1, -1, 277))
        assertEquals(2, videoCollectionScrollIndex(1, 276, 3))
        assertEquals(0, videoCollectionScrollIndex(2, 0, 1))
    }
    @Test fun emptyCollectionDoesNotStartScrolling() {
        for (target in listOf(null, 0, 1, 2)) assertNull(videoCollectionScrollIndex(target, 0, 0))
    }
}
