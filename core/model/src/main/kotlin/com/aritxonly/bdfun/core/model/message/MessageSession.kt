package com.aritxonly.bdfun.core.model.message

/** 消息中心的一个会话（系统通知 / 私信）。 */
data class MessageSession(
    val id: String,
    val talkerId: Long,
    val sessionType: Int,
    val name: String,
    val avatar: String,
    val preview: String,
    val time: Long,
    val unread: Int,
    val isSystem: Boolean,
    val maxSeqno: Long = 0L,
    /** 原始 session_ts（微秒），用于消息列表翻页。 */
    val sessionTs: Long = 0L,
    val preferences: MessageSessionPreferences = MessageSessionPreferences(),
) {
    val followed get() = preferences.followed
    val pinned get() = preferences.pinned
    val muted get() = preferences.muted
}

data class MessageSessionPreferences(
    val followed: Boolean = false,
    val pinned: Boolean = false,
    val muted: Boolean = false,
)
