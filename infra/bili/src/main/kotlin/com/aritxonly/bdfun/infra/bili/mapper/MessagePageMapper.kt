package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.decodeSharedVideo
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal class MessageFeedMapping(
    val items: List<MessageItem>, val rawSize: Int, val nextId: Long, val nextTime: Long, val isEnd: Boolean,
)

internal fun mapMessageFeed(data: JSONObject, type: Int): MessageFeedMapping {
    // 收到的赞返回的是 { latest: { items }, total: { items } } 两个分桶，
    // 不是顶层的 items；原来只读顶层，所以这个页面一直是空的。
    val rawItems = if (type == 2) {
        val merged = ArrayList<JSONObject>()
        data.optJSONObject("latest")?.optJSONArray("items").objects().let(merged::addAll)
        data.optJSONObject("total")?.optJSONArray("items").objects().let(merged::addAll)
        merged.distinctBy { it.s("id") }
    } else {
        data.optJSONArray("items").objects()
    }
    val latestIds = if (type == 2) data.optJSONObject("latest")?.optJSONArray("items")
        .objects().map { it.s("id") }.toSet() else emptySet()
    val parsed = rawItems.mapNotNull { raw ->
        mapMessageFeedItem(MessageFeedItemDto(type, raw))?.let { item ->
            if (type == 2) item.copy(presentation = item.presentation.copy(latestLike = item.id in latestIds)) else item
        }
    }
    val next = (if (type == 2) data.optJSONObject("total") else data)?.optJSONObject("cursor")
    val nextId = next?.l("id") ?: 0L
    val nextTime = next?.l("time") ?: 0L
    return MessageFeedMapping(parsed, rawItems.size, nextId, nextTime,
        next == null || next.optBoolean("is_end", false))
}

internal class MessageSessionMapping(val items: List<MessageSession>, val nextTimestamp: Long, val hasMore: Boolean)

internal fun mapMessageSessions(data: JSONObject, followingOnly: Boolean, pageSize: Int): MessageSessionMapping {
    val allSessions = data.optJSONArray("session_list").objects()
    val raw = if (followingOnly) allSessions.filter { it.optInt("is_follow") == 1 } else allSessions
    val system = raw.map { parseMessageSession(it) }
    val nextTs = allSessions.lastOrNull()?.l("session_ts") ?: 0L
    val more = if (data.has("has_more")) data.optInt("has_more") == 1 else allSessions.size >= pageSize
    return MessageSessionMapping(system, nextTs, more)
}
