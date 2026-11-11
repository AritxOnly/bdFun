package com.aritxonly.bdfun.core.model.message

/** 私信会话里的一条消息。 */
data class ChatMessage(
    val id: Long,
    val senderId: Long,
    val timestamp: Long,
    val text: String,
    val imageUrl: String = "",
    val isMine: Boolean = false,
    val sequence: Long = 0L,
)
