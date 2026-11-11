package com.aritxonly.bdfun.core.model.video

data class VideoDetail(
    val bvid: String,
    val aid: Long,
    val title: String,
    val cover: String,
    val desc: String,
    val ownerName: String,
    val ownerFace: String,
    val ownerMid: Long,
    val ownerSign: String,
    val play: Long,
    val danmaku: Long,
    val like: Long,
    val coin: Long,
    val favorite: Long,
    val share: Long,
    val reply: Long,
    val pubdate: Long,
    val duration: Int,
    val cid: Long,
    val tname: String,
    val pages: List<VideoPage>,
    /** 该视频所属 UGC 合集的全部单集（没有合集时为空）。 */
    val episodes: List<VideoEpisode> = emptyList(),
    /** UGC 合集标题（没有合集时为空）。 */
    val seasonTitle: String = "",
    val isFollowed: Boolean = false,
    val liked: Boolean = false,
    val coined: Boolean = false,
    val favorited: Boolean = false,
)
