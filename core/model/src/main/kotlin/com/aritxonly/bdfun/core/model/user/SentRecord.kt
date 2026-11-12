package com.aritxonly.bdfun.core.model.user

data class SentRecord(
    val id: Long,
    /** "comment" 或 "danmaku" */
    val kind: String,
    val text: String,
    val bvid: String,
    val title: String,
    /** 弹幕在视频里的时间点（毫秒）；评论为 0。 */
    val videoTimeMs: Long,
    /** 发送时间（毫秒时间戳）。 */
    val sentAt: Long,
)
