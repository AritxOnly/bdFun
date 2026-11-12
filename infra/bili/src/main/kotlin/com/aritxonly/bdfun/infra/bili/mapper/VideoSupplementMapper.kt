package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject
import org.json.JSONArray

internal fun mapVideoOnline(data: JSONObject): Pair<Long, String> = data.optLong("count", 0L) to data.s("total")

internal fun mapSubtitleTracks(data: JSONObject): List<SubtitleTrack> {
    val subtitle = data.optJSONObject("subtitle") ?: return emptyList()
    return subtitle.optJSONArray("subtitles").objects().mapNotNull { item ->
        val url = item.s("subtitle_url")
            .ifBlank { item.s("subtitle_url_v2") }
        if (url.isBlank()) return@mapNotNull null
        SubtitleTrack(
            id = item.s("id").ifBlank { item.s("lan") },
            label = item.s("lan_doc").ifBlank { item.s("lan") },
            url = if (url.startsWith("//")) "https:$url" else url,
            isAi = item.optInt("ai_type", 0) > 0,
        )
    }
}

internal fun mapSubtitleCues(text: String): List<SubtitleCue> {
    val root = JSONObject(text)
    return root.optJSONArray("body").objects().mapNotNull { cue ->
        val content = cue.s("content").trim()
        if (content.isBlank()) return@mapNotNull null
        val from = (cue.optDouble("from", 0.0) * 1000).toLong()
        val to = (cue.optDouble("to", 0.0) * 1000).toLong()
        SubtitleCue(from, to.coerceAtLeast(from + 300L), content)
    }
}

internal fun mapSponsorSegments(text: String): List<SponsorSegment> {
    val array = JSONArray(text)
    return buildList {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val segment = obj.optJSONArray("segment") ?: continue
            if (segment.length() < 2) continue
            val start = (segment.optDouble(0, 0.0) * 1000.0).toLong()
            val end = (segment.optDouble(1, 0.0) * 1000.0).toLong()
            if (end <= start) continue
            add(
                SponsorSegment(
                    startMs = start,
                    endMs = end,
                    category = obj.optString("category"),
                    actionType = obj.optString("actionType", "skip"),
                    uuid = obj.optString("UUID"),
                ),
            )
        }
    }
}

internal fun mapCdnSampleUrl(data: JSONObject, isMcdnResource: (String) -> Boolean): String? {
    val rep = data.optJSONObject("dash")
        ?.optJSONArray("video")
        ?.objects()
        ?.firstOrNull()
        ?: return null
    val urls = buildList {
        add(rep.s("baseUrl").ifBlank { rep.s("base_url") })
        addAll(jsonStrings(rep.optJSONArray("backupUrl") ?: rep.optJSONArray("backup_url")))
    }.filter { it.isNotBlank() }
    // 优先用 /upgcxcode/ 形态的地址：mcdn 的 /vN/resource 地址换 host 会 403，不能用来测速。
    return urls.firstOrNull { it.contains("/upgcxcode/") && !isMcdnResource(it) }
        ?: urls.firstOrNull { it.contains("/upgcxcode/") }
        ?: urls.firstOrNull()
}
