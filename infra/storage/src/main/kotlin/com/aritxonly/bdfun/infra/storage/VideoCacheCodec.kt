package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.model.cache.*
import org.json.JSONArray
import org.json.JSONObject

/** 旧平铺 JSON 保持字段名/默认值/顺序；单条异常仍按原逻辑使整份读取回退为空。 */
internal fun decodeVideoCache(raw: String, exists: (String) -> Boolean): List<CachedVideo> = runCatching {
    val result = ArrayList<CachedVideo>()
    val array = JSONArray(raw)
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        var entry = cachedVideoFromJson(item)
        if (entry.status == CacheStatus.Queued || entry.status == CacheStatus.Downloading) {
            entry = entry.copy(transfer = entry.transfer.copy(status = CacheStatus.Failed), error = "下载已中断")
        }
        if (entry.status == CacheStatus.Completed &&
            (!exists(entry.videoPath) || (entry.audioPath.isNotBlank() && !exists(entry.audioPath)))) {
            entry = entry.copy(transfer = entry.transfer.copy(status = CacheStatus.Failed), error = "缓存文件已丢失")
        }
        if (entry.id.isNotBlank()) result.add(entry)
    }
    result
}.getOrDefault(emptyList())

private fun cachedVideoFromJson(item: JSONObject) = CachedVideo(
    id = item.optString("id"),
    video = CacheVideoInfo(item.optString("bvid"), item.optLong("aid"), item.optLong("cid"), item.optString("title"),
        item.optString("cover"), item.optString("ownerName"), item.optLong("ownerMid"), item.optInt("durationSec")),
    files = CacheVideoFiles(item.optInt("qualityId"), item.optString("qualityLabel"), item.optString("videoPath"),
        item.optString("audioPath"), item.optString("coverPath")),
    transfer = CacheTransferState(item.optDouble("progress", 0.0).toFloat(), item.optLong("bytes"), item.optLong("totalBytes"),
        runCatching { CacheStatus.valueOf(item.optString("status")) }.getOrDefault(CacheStatus.Failed)),
    createdAt = item.optLong("createdAt"), error = item.optString("error").ifBlank { null },
)
internal fun encodeVideoCache(entries: List<CachedVideo>): String = JSONArray().apply {
    entries.forEach { put(it.toCacheJson()) }
}.toString()
private fun CachedVideo.toCacheJson() = JSONObject().apply {
    put("id", id)
    put("bvid", bvid)
    put("aid", aid)
    put("cid", cid)
    put("title", title)
    put("cover", cover)
    put("coverPath", coverPath)
    put("ownerName", ownerName)
    put("ownerMid", ownerMid)
    put("durationSec", durationSec)
    put("qualityId", qualityId)
    put("qualityLabel", qualityLabel)
    put("videoPath", videoPath)
    put("audioPath", audioPath)
    put("progress", progress.toDouble())
    put("bytes", bytes)
    put("totalBytes", totalBytes)
    put("status", status.name)
    put("createdAt", createdAt)
    put("error", error.orEmpty())
}
