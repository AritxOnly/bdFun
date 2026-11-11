package com.aritxonly.bdfun.core.model.video

/** 合集里的单集（`ugc_season`），可能属于另一个 bvid。 */
data class VideoEpisode(
    val bvid: String,
    val aid: Long,
    val cid: Long,
    val title: String,
    val cover: String,
    val duration: Int,
    val play: Long = 0L,
    val danmaku: Long = 0L,
    val pubdate: Long = 0L,
)
