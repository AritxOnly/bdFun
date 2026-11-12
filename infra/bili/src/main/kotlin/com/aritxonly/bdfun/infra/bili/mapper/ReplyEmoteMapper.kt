package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.comment.EmoteItem
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun mapReplyEmotes(data: JSONObject): List<EmoteItem> {
    val out = mutableListOf<EmoteItem>()
    data.optJSONArray("packages")?.objects()?.forEach { pkg ->
        pkg.optJSONArray("emote")?.objects()?.forEach { emote ->
            val text = emote.s("text").removeSurrounding("[", "]").trim()
            val url = image(emote.s("url"), 120)
            if (text.isNotBlank() && url.isNotBlank()) {
                out += EmoteItem(text = text, url = url)
            }
        }
    }
    return out
}
