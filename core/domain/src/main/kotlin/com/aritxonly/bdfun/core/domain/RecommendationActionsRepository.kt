package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.RecommendationReason
import com.aritxonly.bdfun.core.model.video.VideoItem

interface RecommendationActionsRepository {
    suspend fun addWatchLater(aid: Long): Outcome<Boolean>
    suspend fun dislike(video: VideoItem, reason: RecommendationReason): Outcome<Unit>
}
