package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.decodeSharedVideo
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun mapPrivateMessagePage(data: JSONObject, own: Long, before: Long?): ChatMessagePage {
    val items = data.optJSONArray("messages").objects().mapNotNull { message ->
        val type = message.optInt("msg_type", 1)
        if (type == 5) return@mapNotNull null
        val raw = message.s("content")
        val content = cancellableCatching { JSONObject(raw) }.getOrNull()
        val withdrawn = message.optInt("msg_status") == 1
        val text = if (withdrawn) "消息已撤回" else when (type) {
            1 -> content?.s("content").orEmpty()
            2, 6 -> ""
            6, 7 -> content?.s("title").orEmpty()
            else -> content?.s("content").orEmpty().ifBlank { raw }
        }
        val image = if (type in listOf(2, 6) && !withdrawn) content?.s("url").orEmpty() else ""
        val card = if (type in listOf(7, 10, 11, 12, 13, 14, 16, 18) && content != null && !withdrawn) mapChatCard(content, type) else null
        if (text.isBlank() && image.isBlank() && type != 1 && card == null) return@mapNotNull null
        ChatMessage(
            id = message.l("msg_key").takeIf { it != 0L }
                ?: message.l("msg_seqno"),
            sequence = message.l("msg_seqno"),
            senderId = message.l("sender_uid"),
            timestamp = message.l("timestamp").takeIf { it > 0L }
                ?: message.l("msg_time"),
            text = text,
            card = card, withdrawn = withdrawn, type = type, source = message.optInt("msg_source"),
            sharedVideo = decodeSharedVideo(text),
            imageUrl = image,
            isMine = message.l("sender_uid") == own,
        )
    }.sortedBy { it.sequence }
    val next = data.optString("min_seqno").toLongOrNull() ?: 0L
    return ChatMessagePage(items, sessionContinuation(before ?: 0L, next, data.optInt("has_more") == 1),
        data.optJSONArray("e_infos").objects().associate { it.s("text") to it.s("gif_url").ifBlank { it.s("url") } })
}
