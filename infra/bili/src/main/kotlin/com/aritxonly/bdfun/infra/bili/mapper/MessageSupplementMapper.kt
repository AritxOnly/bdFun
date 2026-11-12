package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.decodeSharedVideo
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun mapNewFollowers(data: JSONObject): List<MessageItem> {
    return data.optJSONArray("list").objects().mapNotNull { fan ->
        val fanMid = fan.l("mid")
        if (fanMid <= 0L) return@mapNotNull null
        val attribute = fan.optInt("attribute", 0)
        MessageItem(
            id = "fans_$fanMid",
            title = "",
            content = fan.s("sign"),
            time = fan.l("mtime").takeIf { it > 0L } ?: fan.l("ctime"),
            name = fan.s("uname"),
            avatar = avatar(fan.s("face"), 120),
            presentation = MessagePresentation(
                action = "关注了我",
                source = fan.s("sign"),
            ),
            actor = MessageActor(
                userMid = fanMid,
                followed = attribute == 2 || attribute == 6 ||
                fan.optBoolean("is_follow", false),
            ),
        )
    }
}

internal fun mapSystemMessages(root: JSONObject): List<MessageItem> {
    return root.optJSONArray("data").objects().map {
        val raw = it.s("content")
        val content = cancellableCatching { JSONObject(raw).optString("web", raw) }.getOrDefault(raw)
        MessageItem(
            id = it.s("id"),
            title = it.s("title"),
            content = content,
            time = 0L,
            name = "系统通知",
            avatar = "",
            presentation = MessagePresentation(
                source = it.s("time_at"),
            ),
            notice = MessageNotice(
                systemCursor = it.l("cursor"),
            ),
        )
    }
}
