package com.aritxonly.bdfun.infra.bili.message

import com.aritxonly.bdfun.core.model.message.*
import org.json.JSONArray
import org.json.JSONObject

internal fun JSONArray?.imObjects(): List<JSONObject> = if (this == null) emptyList() else
    (0 until length()).mapNotNull { optJSONObject(it) }

internal fun mapImInbox(data: JSONObject, previous: String?): MessageInboxPage {
    val pagination = data.optJSONObject("pagination_params")
    val cursor = pagination?.toString()?.takeIf { pagination.optBoolean("has_more") && it != previous }
    return MessageInboxPage(data.optJSONArray("sessions").imObjects().map(::mapImSession), cursor,
        mapImMenu(data.optJSONArray("three_dot_items")), mapImMenu(data.optJSONArray("outside_item")))
}

private fun mapImMenu(items: JSONArray?) = items.imObjects().map {
    MessageMenuItem(it.optInt("type"), it.optString("title"), it.optString("url"), it.optString("icon"))
}

internal fun mapImSession(value: JSONObject): MessageSession {
    val id = value.optJSONObject("id") ?: JSONObject()
    val info = value.optJSONObject("session_info") ?: JSONObject()
    val talker = id.optJSONObject("private_id")?.optLong("talker_uid") ?: 0
    val folded = id.optJSONObject("fold_id")?.optInt("type")
    val page = when (folded) { 2, 3 -> 5; 4 -> 2; 5 -> 3; 6 -> 4; 7, 10 -> 8; 9 -> 7; else -> null }
    return MessageSession(id.toString(), talker, if (talker > 0) 1 else 0,
        info.optString("session_name"), findAvatarUrl(info.optJSONObject("avatar")),
        value.optJSONObject("msg_summary")?.optString("raw_msg").orEmpty(),
        value.optLong("timestamp") / 1_000_000, value.optJSONObject("unread")?.optInt("number") ?: 0,
        id.optJSONObject("system_id")?.optInt("type") == 8, maxSeqno = value.optLong("sequence_number"), sessionTs = value.optLong("timestamp"),
        preferences = MessageSessionPreferences(pinned = value.optBoolean("is_pinned"), muted = value.optBoolean("is_muted"),
            identity = id.toString(), pageType = page, chatUrl = value.optString("chat_url"),
            avatarMid = info.optJSONObject("avatar")?.optLong("mid") ?: 0,
            unreadDot = value.optJSONObject("unread")?.optInt("style") == 1))
}

private fun findAvatarUrl(value: Any?): String {
    if (value is JSONObject) {
        val remote = value.optJSONObject("remote")?.optString("url").orEmpty()
        if (remote.isNotBlank()) return remote
        for (key in value.keys()) findAvatarUrl(value.opt(key)).takeIf { it.isNotBlank() }?.let { return it }
    } else if (value is JSONArray) {
        for (index in 0 until value.length()) findAvatarUrl(value.opt(index)).takeIf { it.isNotBlank() }?.let { return it }
    }
    return ""
}
