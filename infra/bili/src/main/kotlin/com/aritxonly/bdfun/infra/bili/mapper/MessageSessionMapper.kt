package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.decodeSharedVideo
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun parseMessageSession(message: JSONObject): MessageSession = with(message) {
    val type = optInt("session_type", 1)
    val systemType = optInt("system_msg_type", 0)
    val account = optJSONObject("account_info")
    val name = account?.s("name").orEmpty()
        .ifBlank { if (type == 2) s("group_name") else "" }
    val avatar = account?.s("pic_url").orEmpty()
        .ifBlank { if (type == 2) s("group_cover") else "" }
    val last = optJSONObject("last_msg")
    MessageSession(
        id = "${type}_${l("talker_id")}",
        talkerId = l("talker_id"),
        sessionType = type,
        name = name,
        avatar = avatar,
        preview = last?.let { previewOfMessage(it) }.orEmpty(),
        time = (l("session_ts") / 1_000_000L).takeIf { it > 0L }
            ?: (last?.l("timestamp") ?: 0L),
        unread = optInt("unread_count", 0),
        isSystem = systemType != 0,
        preferences = MessageSessionPreferences(
            followed = optInt("is_follow") == 1,
            pinned = l("top_ts") > 0L,
            muted = optInt("is_dnd") == 1,
        ),
        maxSeqno = l("max_seqno"),
        sessionTs = l("session_ts"),
    )
}

internal fun previewOfMessage(message: JSONObject): String {
    val type = message.optInt("msg_type", 0)
    val raw = message.optString("content")
    val obj = cancellableCatching { JSONObject(raw) }.getOrNull()
    return when (type) {
        2 -> "[图片]"
        306 -> obj?.s("content").orEmpty()
        10, 11, 12 -> obj?.s("title").orEmpty()
            .ifBlank { obj?.s("text").orEmpty() }
            .ifBlank { obj?.s("desc").orEmpty() }
        else -> obj?.s("content").orEmpty()
            .ifBlank { obj?.s("title").orEmpty() }
            .ifBlank { obj?.s("text").orEmpty() }
            .ifBlank { raw }
    }.replace('\n', ' ')
}
