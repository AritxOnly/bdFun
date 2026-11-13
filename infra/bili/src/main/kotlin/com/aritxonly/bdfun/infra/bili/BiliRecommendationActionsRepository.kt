package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.RecommendationActionsRepository
import com.aritxonly.bdfun.core.model.video.RecommendationReason
import com.aritxonly.bdfun.core.model.video.VideoItem

class BiliRecommendationActionsRepository(
    private val watchLater: suspend (Long) -> Boolean,
    private val feedback: suspend (VideoItem, RecommendationReason) -> Unit,
) : RecommendationActionsRepository {
    override suspend fun addWatchLater(aid: Long) = biliOutcome { watchLater(aid) }
    override suspend fun dislike(video: VideoItem, reason: RecommendationReason) = biliOutcome { feedback(video, reason) }
}
