package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.VideoItem

/** Immediate in-memory exclusion while the server updates the recommendation model. */
class RecommendationExclusions {
    private val videos = HashSet<String>()
    private val creators = HashSet<Long>()
    private val categories = HashSet<String>()
    val hasCategories: Boolean get() = categories.isNotEmpty()
    val isEmpty: Boolean get() = videos.isEmpty()
    fun exclude(video: VideoItem, creator: Boolean = false, category: String? = null) {
        videos.add(video.bvid)
        if (creator && video.ownerMid > 0) creators.add(video.ownerMid)
        category?.takeIf { it.isNotBlank() }?.let(categories::add)
    }
    fun accepts(video: VideoItem): Boolean = video.bvid !in videos &&
        video.ownerMid !in creators && video.categoryName !in categories
}
