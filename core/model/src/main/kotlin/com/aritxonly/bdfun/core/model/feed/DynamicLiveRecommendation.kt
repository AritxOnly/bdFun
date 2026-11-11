package com.aritxonly.bdfun.core.model.feed

data class DynamicLiveRecommendation(
    val roomId: Long, val title: String, val cover: String,
    val live: Boolean, val areaName: String, val watched: String,
)
