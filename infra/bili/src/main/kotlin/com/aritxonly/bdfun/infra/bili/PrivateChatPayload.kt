package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.ChatUploadedImage
import com.aritxonly.bdfun.core.model.message.SharedVideoInfo
import org.json.JSONObject

internal fun decodeChatUploadedImage(raw: String?): ChatUploadedImage {
    val uploaded = JSONObject(raw)
    val url = uploaded.optString("img_src")
    check(url.isNotBlank()) { "图片上传失败" }
    return ChatUploadedImage(url, uploaded.optInt("img_width"), uploaded.optInt("img_height"),
        uploaded.optDouble("img_size", 0.0))
}

internal fun encodeChatImage(image: ChatUploadedImage): String = JSONObject()
    .put("url", image.url).put("width", image.width).put("height", image.height)
    .put("size", image.size).put("imageType", image.imageType).put("original", 1).toString()

internal fun decodeSharedVideo(text: String): SharedVideoInfo? {
    val trimmed = text.trim()
    if (!trimmed.startsWith("{")) return null
    return runCatching {
        val json = JSONObject(trimmed)
        val bvid = json.optString("bvid")
        if (bvid.isBlank()) return@runCatching null
        SharedVideoInfo(bvid, json.optString("title").ifBlank { "视频" }, json.optString("cover"),
            json.optLong("view"), json.optLong("danmaku"), json.optString("desc"))
    }.getOrNull()
}
