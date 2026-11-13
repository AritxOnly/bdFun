package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.live.LiveRoomInfo
import org.json.JSONObject

internal fun mapLiveRoomInfo(json: JSONObject, roomId: Long): LiveRoomInfo? {
    if (json.optInt("code", -1) != 0) return null
    val data = json.optJSONObject("data") ?: return null
    val info = data.optJSONObject("room_info") ?: return null
    val anchor = data.optJSONObject("anchor_info")?.optJSONObject("base_info")
    return LiveRoomInfo(
        roomId = info.optLong("room_id", roomId),
        title = info.optString("title"),
        uname = anchor?.optString("uname").orEmpty(),
        uid = info.optLong("uid"),
        face = normalizeLiveUrl(anchor?.optString("face").orEmpty()),
        cover = normalizeLiveUrl(
            info.optString("background").ifBlank {
                info.optString("cover").ifBlank { info.optString("keyframe") }
            },
        ),
        onlineText = liveCountText(info.optLong("online")),
        areaText = info.optString("area_name"),
        following = false,
        live = info.optInt("live_status", 1) == 1,
    )
}

internal fun readLiveHotRoomId(json: JSONObject): Long {
    return json.optJSONObject("data")
            ?.optJSONArray("list")
            ?.optJSONObject(0)
            ?.optLong("roomid", 0L) ?: 0L
}

internal fun mapLivePortraitBackground(json: JSONObject): String? {
    val room = json.optJSONObject("data")?.optJSONObject("room_info")
        ?: return null
    return normalizeLiveUrl(
        room.optString("app_background").ifBlank { room.optString("background") },
    ).ifBlank { null }
}

internal fun mapLiveQualities(json: JSONObject): List<Pair<Int, String>> {
    val playurl = json.optJSONObject("data")
        ?.optJSONObject("playurl_info")
        ?.optJSONObject("playurl")
        ?: return emptyList()

    val descMap = LinkedHashMap<Int, String>()
    playurl.optJSONArray("g_qn_desc")?.let { desc ->
        for (i in 0 until desc.length()) {
            val item = desc.optJSONObject(i) ?: continue
            val qn = item.optInt("qn", 0)
            val name = item.optString("desc")
            if (qn > 0 && name.isNotBlank()) descMap.putIfAbsent(qn, name)
        }
    }

    val acceptQn = LinkedHashSet<Int>()
    val streams = playurl.optJSONArray("stream")
    if (streams != null) {
        for (s in 0 until streams.length()) {
            val formats = streams.optJSONObject(s)?.optJSONArray("format") ?: continue
            for (f in 0 until formats.length()) {
                val codecs = formats.optJSONObject(f)?.optJSONArray("codec") ?: continue
                for (c in 0 until codecs.length()) {
                    val list = codecs.optJSONObject(c)?.optJSONArray("accept_qn") ?: continue
                    for (q in 0 until list.length()) {
                        val qn = list.optInt(q, 0)
                        if (qn > 0) acceptQn += qn
                    }
                }
            }
        }
    }

    if (acceptQn.isEmpty()) {
        return descMap.map { (qn, name) -> qn to name }
    }
    val all = acceptQn.map { qn ->
        qn to (descMap[qn] ?: liveQualityFallbackName(qn))
    }
    // 和 BiliPai 实际展示保持一致：普通直播间只列 原画 / 蓝光 / 超清。
    // 低清晰度房间没有这三档时再退回完整列表，避免菜单空掉。
    return all.filter { it.first == 10000 || it.first == 400 || it.first == 250 }
        .ifEmpty { all }
}

private fun liveQualityFallbackName(qn: Int): String = when (qn) {
    30000 -> "杜比"
    20000 -> "4K"
    15000 -> "2K"
    10000 -> "原画"
    400 -> "蓝光"
    250 -> "超清"
    150 -> "高清"
    80 -> "流畅"
    else -> qn.toString()
}


internal fun mapLiveEmoticons(json: JSONObject): Map<String, String> {
    if (json.optInt("code", -1) != 0) return emptyMap()
    val packages = json.optJSONObject("data")?.optJSONArray("data") ?: return emptyMap()
    return buildMap {
        for (i in 0 until packages.length()) {
            val emoticons = packages.optJSONObject(i)?.optJSONArray("emoticons") ?: continue
            for (e in 0 until emoticons.length()) {
                val item = emoticons.optJSONObject(e) ?: continue
                val emoji = item.optString("emoji").removeSurrounding("[", "]")
                val url = item.optString("url")
                if (emoji.isNotBlank() && url.isNotBlank()) put(emoji, url)
            }
        }
    }
}

internal fun mapLiveDanmuInfo(json: JSONObject?): Pair<String, List<String>>? {
    if (json == null || json.optInt("code", -1) != 0) return null
    val data = json.optJSONObject("data") ?: return null
    val token = data.optString("token")
    val hosts = data.optJSONArray("host_list") ?: return null
    // 和 BiliPai 一致：443 的 wss 优先，其次其它 wss，最后退回 ws
    data class Host(val host: String, val wssPort: Int, val wsPort: Int)
    val list = buildList {
        for (i in 0 until hosts.length()) {
            val h = hosts.optJSONObject(i) ?: continue
            add(
                Host(
                    host = h.optString("host"),
                    wssPort = h.optInt("wss_port", 0),
                    wsPort = h.optInt("ws_port", 0),
                ),
            )
        }
    }.filter { it.host.isNotBlank() }
        .sortedBy { when {
            it.wssPort == 443 -> 0
            it.wssPort != 0 -> 1
            it.wsPort != 0 -> 2
            else -> 3
        } }
    val urls = list.mapNotNull { h ->
        val port = if (h.wssPort != 0) h.wssPort else h.wsPort
        if (port == 0) null
        else "${if (h.wssPort != 0) "wss" else "ws"}://${h.host}:$port/sub"
    }.distinct()
    return if (token.isBlank() || urls.isEmpty()) null else token to urls
}


internal fun parseLivePlayUrl(json: JSONObject): String? {
    val streams = json.optJSONObject("data")
        ?.optJSONObject("playurl_info")
        ?.optJSONObject("playurl")
        ?.optJSONArray("stream") ?: return null
    var fallback: String? = null
    for (i in 0 until streams.length()) {
        val formats = streams.optJSONObject(i)?.optJSONArray("format") ?: continue
        for (j in 0 until formats.length()) {
            val formatObj = formats.optJSONObject(j) ?: continue
            val formatName = formatObj.optString("format_name")
            val codecs = formatObj.optJSONArray("codec") ?: continue
            for (k in 0 until codecs.length()) {
                val codec = codecs.optJSONObject(k) ?: continue
                val codecName = codec.optString("codec_name")
                val infos = codec.optJSONArray("url_info") ?: continue
                val first = infos.optJSONObject(0) ?: continue
                val url = first.optString("host") +
                    codec.optString("base_url") +
                    first.optString("extra")
                if (url.isBlank()) continue
                if (fallback == null) fallback = url
                // 直播优先 FLV + AVC：兼容性最好，Media3 直接能播
                if (codecName == "avc" && formatName == "flv") {
                    return url
                }
            }
        }
    }
    return fallback
}

/** 直播接口有时返回 //i0.hdslb.com/... 或 http:// 这种地址，统一升级到 https。 */
private fun normalizeLiveUrl(url: String): String = when {
    url.isBlank() -> ""
    url.startsWith("//") -> "https:$url"
    url.startsWith("http://") -> "https://" + url.removePrefix("http://")
    else -> url
}

/** 人气数：1.2 万 / 356 这种中文习惯写法。 */
private fun liveCountText(count: Long): String = when {
    count <= 0 -> ""
    count >= 100_000_000 -> String.format(java.util.Locale.US, "%.1f 亿", count / 100_000_000.0)
    count >= 10_000 -> String.format(java.util.Locale.US, "%.1f 万", count / 10_000.0)
    else -> count.toString()
}

internal fun readLiveRoomId(json: JSONObject): Long = json.optJSONObject("data")?.optLong("room_id", 0L) ?: 0L
