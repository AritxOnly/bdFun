package com.aritxonly.bdfun.core.model.video

data class VideoItem(
    val bvid: String,
    val aid: Long,
    val title: String,
    val cover: String,
    val ownerName: String,
    val ownerFace: String,
    val ownerMid: Long,
    val play: Long,
    val danmaku: Long,
    val duration: Int,
    val pubdate: Long,
    val cid: Long = 0L,
    val reason: String = "",
    val isFollowed: Boolean = false,
    /** 从「我的评论与弹幕」等入口跳转时，进入视频后直接定位到这个时间点。 */
    val startPositionMs: Long = 0L,
    val categoryName: String = "",
    val recommendationTrackId: String = "",
    val appRecommendation: Boolean = false,
    val dislikeReasons: List<RecommendationReason> = emptyList(),
)
