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
    val sharedVideo: SharedVideoInfo? = null,
    val card: ChatMessageCard? = null,
    val withdrawn: Boolean = false,
    val type: Int = 1,
    val source: Int = 0,
)

data class ChatMessageCard(val title: String, val text: String = "", val image: String = "", val url: String = "", val children: List<ChatMessageCard> = emptyList())
