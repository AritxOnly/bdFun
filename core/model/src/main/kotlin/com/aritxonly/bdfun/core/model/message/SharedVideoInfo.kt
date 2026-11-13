package com.aritxonly.bdfun.core.model.message

/** 私信文本中原有的视频分享快照。 */
data class SharedVideoInfo(
    val bvid: String, val title: String, val cover: String,
    val play: Long, val danmaku: Long, val desc: String,
)
