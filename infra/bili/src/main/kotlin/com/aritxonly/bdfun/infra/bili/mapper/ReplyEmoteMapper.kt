package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.comment.EmoteItem
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun mapReplyEmotes(data: JSONObject): List<EmoteItem> {
    val out = mutableListOf<EmoteItem>()
    data.optJSONArray("packages")?.objects()?.forEachIndexed { index, pkg ->
        pkg.optJSONArray("emote")?.objects()?.forEach { emote ->
            val text = emote.s("text").removeSurrounding("[", "]").trim()
            // 表情保留原始比例，不能使用视频封面的 16:9 裁切参数。
            val url = image(emote.s("url"))
            val type = pkg.optInt("type", 1)
            if (text.isNotBlank() && (type == 4 || url.isNotBlank())) {
                out += EmoteItem(text = text, url = url, packageId = index,
                    packageIcon = image(pkg.s("url")), type = type,
                    size = emote.optJSONObject("meta")?.optInt("size", 1) ?: 1)
            }
        }
    }
    return out
}
