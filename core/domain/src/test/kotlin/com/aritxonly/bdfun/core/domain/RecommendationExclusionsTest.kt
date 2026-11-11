package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.VideoItem

import org.junit.Assert.*
import org.junit.Test

class RecommendationExclusionsTest {
    private fun video(id: String, creator: Long = 1, category: String = "游戏") =
        VideoItem(id, 1, "", "", "", "", creator, 0, 0, 0, 0, categoryName = category)
    @Test fun currentVideoDoesNotExcludeTheWholeCreator() {
        val filter = RecommendationExclusions()
        filter.exclude(video("one"))
        assertFalse(filter.accepts(video("one")))
        assertTrue(filter.accepts(video("two")))
    }
    @Test fun creatorRejectsOtherVideosButNeverAllUnknownAuthors() {
        val filter = RecommendationExclusions()
        filter.exclude(video("one"), creator = true)
        filter.exclude(video("unknown", 0), creator = true)
        assertFalse(filter.accepts(video("two")))
        assertTrue(filter.accepts(video("other", 2)))
        assertTrue(filter.accepts(video("other-unknown", 0)))
    }
    @Test fun categoryIsActualMetadataAndBlankIsNotAFilter() {
        val filter = RecommendationExclusions()
        filter.exclude(video("one"), category = "游戏")
        filter.exclude(video("two"), category = "")
        assertFalse(filter.accepts(video("game", 2)))
        assertTrue(filter.accepts(video("car", 2, "汽车")))
        assertTrue(filter.accepts(video("missing", 2, "")))
    }
}
