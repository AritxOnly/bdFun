package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.message.ChatMessageCard
import org.json.JSONArray
import org.json.JSONObject

internal fun mapChatCard(value: JSONObject, type: Int): ChatMessageCard {
    val url = when (type) {
        11 -> "https://www.bilibili.com/video/${value.s("bvid") }"
        12 -> "https://www.bilibili.com/read/cv${value.s("rid") }"
        14 -> if (value.s("source") == "直播") "https://live.bilibili.com/${value.s("sourceID")}" else ""
        7 -> when (value.optInt("source")) {
            2, 11 -> "https://t.bilibili.com/${value.s("id") }"
            5 -> "https://www.bilibili.com/video/${value.s("bvid").ifBlank { "av${value.s("id")}" }}"
            6 -> "https://www.bilibili.com/read/cv${value.s("id") }"
            16 -> "https://www.bilibili.com/bangumi/play/ep${value.s("id") }"
            else -> ""
        }
        else -> value.s("jump_uri").ifBlank { value.s("jump_url") }
    }
    val text = when (type) {
        18 -> runCatching { JSONArray(value.s("content")).objects().joinToString("\n") { it.s("text") } }
            .getOrElse { value.s("content") }
        10 -> listOf(value.s("text"), value.optJSONArray("modules").objects()
            .joinToString("\n") { "${it.s("title")} ${it.s("detail")}" }).filter { it.isNotBlank() }.joinToString("\n")
        else -> value.s("summary").ifBlank { value.s("headline") }.ifBlank { value.s("author") }
    }
    val links = listOf("", "_2", "_3").mapNotNull { suffix ->
        value.s("jump_uri$suffix").takeIf { it.isNotBlank() }?.let {
            ChatMessageCard(value.s("jump_text$suffix").ifBlank { "查看详情" }, url = it)
        }
    }
    val children = if (type == 16) value.optJSONArray("sub_cards").objects().map {
        ChatMessageCard(it.s("title"), image = it.s("cover"), url = it.s("jump_url"))
    } else links
    return ChatMessageCard(value.s("title").ifBlank { value.s("main_title") }, text,
        value.s("cover").ifBlank { value.s("thumb") }.ifBlank { value.s("pic_url") }
            .ifBlank { value.optJSONArray("image_urls")?.optString(0).orEmpty() }, url, children)
}
