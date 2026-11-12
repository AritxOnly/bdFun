package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.live.LiveRoomInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ---------------------------------------------------------------- live

private val liveBase = "https://api.live.bilibili.com"

/**
 * 直播间信息（标题 / 主播 / 封面 / 人气 / 分区）。
 * 接口不通时返回 null，界面会退回 mock 数据，不会白屏。
 */
internal suspend fun BiliApiContext.liveRoomInfo(roomId: Long): LiveRoomInfo? = withContext(Dispatchers.IO) {
    val referer = "https://live.bilibili.com/$roomId"
    val json = runCatching {
        rawRequest(
            "/xlive/web-room/v1/index/getInfoByRoom",
            mapOf("room_id" to roomId.toString()),
            base = liveBase,
            referer = referer,
        )
    }.getOrNull() ?: return@withContext null
    if (json.optInt("code", -1) != 0) return@withContext null
    val data = json.optJSONObject("data") ?: return@withContext null
    val info = data.optJSONObject("room_info") ?: return@withContext null
    val anchor = data.optJSONObject("anchor_info")?.optJSONObject("base_info")
    LiveRoomInfo(
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

/**
 * 随便挑一个正在直播的房间号（首页推荐列表第一条）。
 * 用于「没有指定房间也能看看效果」；全站都没开播时返回 null。
 */
internal suspend fun BiliApiContext.liveHotRoomId(): Long? = withContext(Dispatchers.IO) {
    for (area in listOf("1", "9", "0")) {
        val json = runCatching {
            rawRequest(
                "/xlive/web-interface/v1/second/getList",
                mapOf(
                    "platform" to "web",
                    "parent_area_id" to area,
                    "area_id" to "0",
                    "sort_type" to "online",
                    "page" to "1",
                    "page_size" to "1",
                ),
                base = liveBase,
                referer = "https://live.bilibili.com/",
            )
        }.getOrNull() ?: continue
        val id = json.optJSONObject("data")
            ?.optJSONArray("list")
            ?.optJSONObject(0)
            ?.optLong("roomid", 0L) ?: 0L
        if (id > 0L) return@withContext id
    }
    null
}

/**
 * 竖屏用的房间背景（H5 接口的 app_background），和 BiliPai 取值一致。
 */
internal suspend fun BiliApiContext.livePortraitBackground(roomId: Long): String? = withContext(Dispatchers.IO) {
    val referer = "https://live.bilibili.com/$roomId"
    val json = runCatching {
        rawRequest(
            "/xlive/web-room/v1/index/getH5InfoByRoom",
            mapOf("room_id" to roomId.toString()),
            base = liveBase,
            referer = referer,
        )
    }.getOrNull() ?: return@withContext null
    val room = json.optJSONObject("data")?.optJSONObject("room_info")
        ?: return@withContext null
    normalizeLiveUrl(
        room.optString("app_background").ifBlank { room.optString("background") },
    ).ifBlank { null }
}

/**
 * 可用清晰度列表（qn → 描述）。
 *
 * 不能直接用 g_qn_desc：那是服务端支持的画质字典，包含 4K / 杜比等当前直播间
 * 根本没有推流的档位。真正的可播档位在 stream[].format[].codec[].accept_qn 里。
 */
internal suspend fun BiliApiContext.liveQualities(roomId: Long): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
    val params = mapOf(
        "room_id" to roomId.toString(),
        "protocol" to "0,1",
        "format" to "0,1,2",
        "codec" to "0,1",
        "qn" to "10000",
        "platform" to "web",
        "ptype" to "8",
    )
    val json = runCatching {
        rawRequest(
            "/xlive/web-room/v2/index/getRoomPlayInfo", params,
            base = liveBase, referer = "https://live.bilibili.com/$roomId",
        )
    }.getOrNull() ?: return@withContext emptyList()
    val playurl = json.optJSONObject("data")
        ?.optJSONObject("playurl_info")
        ?.optJSONObject("playurl")
        ?: return@withContext emptyList()

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
        return@withContext descMap.map { (qn, name) -> qn to name }
    }
    val all = acceptQn.map { qn ->
        qn to (descMap[qn] ?: liveQualityFallbackName(qn))
    }
    // 和 BiliPai 实际展示保持一致：普通直播间只列 原画 / 蓝光 / 超清。
    // 低清晰度房间没有这三档时再退回完整列表，避免菜单空掉。
    all.filter { it.first == 10000 || it.first == 400 || it.first == 250 }
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

/**
 * 直播弹幕表情包：emoji 文本 → 图片 URL。
 *
 * 消息里的 extra.emots 只覆盖本条消息附带的表情；房间表情包需要单独拉取，
 * 否则 [doge] 这类常见表情只会显示成文字。
 */
internal suspend fun BiliApiContext.liveEmoticons(roomId: Long): Map<String, String> = withContext(Dispatchers.IO) {
    val json = runCatching {
        rawRequest(
            "/xlive/web-ucenter/v2/emoticon/GetEmoticons",
            mapOf("platform" to "pc", "room_id" to roomId.toString()),
            base = liveBase,
            referer = "https://live.bilibili.com/$roomId",
        )
    }.getOrNull() ?: return@withContext emptyMap()
    if (json.optInt("code", -1) != 0) return@withContext emptyMap()
    val packages = json.optJSONObject("data")?.optJSONArray("data") ?: return@withContext emptyMap()
    buildMap {
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

/**
 * 弹幕服务器：token + wss 地址列表。
 */
internal suspend fun BiliApiContext.liveDanmuInfo(roomId: Long): Pair<String, List<String>>? = withContext(Dispatchers.IO) {
    val realRoomId = runCatching {
        rawRequest(
            "/room/v1/Room/room_init",
            mapOf("id" to roomId.toString()),
            base = liveBase,
            referer = "https://live.bilibili.com/$roomId",
        ).optJSONObject("data")?.optLong("room_id", 0L) ?: 0L
    }.getOrDefault(0L).takeIf { it > 0L } ?: roomId

    val referer = "https://live.bilibili.com/$realRoomId"
    val params = mapOf(
        "id" to realRoomId.toString(),
        "type" to "0",
        "web_location" to "444.8",
    )

    fun parse(json: JSONObject?): Pair<String, List<String>>? {
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

    // 和 BiliPai 一致：这个接口必须带 WBI 签名（否则 -352），失败就刷新密钥重试一次
    suspend fun signed(): JSONObject? = runCatching {
        rawRequest(
            "/xlive/web-room/v1/index/getDanmuInfo", params,
            signed = true, base = liveBase, referer = referer,
        )
    }.getOrNull()

    parse(signed())?.let { return@withContext it }
    // 刷新 WBI 密钥后重试
    wbiSigner.clear()
    parse(signed())
}

/**
 * 直播流地址。先按 web 端参数要一遍；失败时再带 WBI 签名重试一次。
 */
internal suspend fun BiliApiContext.livePlayUrl(roomId: Long, qn: Int = 10000): String? = withContext(Dispatchers.IO) {
    val referer = "https://live.bilibili.com/$roomId"
    val params = mapOf(
        "room_id" to roomId.toString(),
        "protocol" to "0,1",
        "format" to "0,1,2",
        "codec" to "0,1",
        "qn" to qn.toString(),
        "platform" to "web",
        "ptype" to "8",
        "dolby" to "5",
        "panorama" to "1",
        "web_location" to "444.8",
    )
    fun pick(json: JSONObject?): String? = json?.let(::parseLivePlayUrl)
    val plain = runCatching {
        rawRequest(
            "/xlive/web-room/v2/index/getRoomPlayInfo",
            params,
            base = liveBase,
            referer = referer,
        )
    }.getOrNull()
    pick(plain)?.let { return@withContext it }
    val signed = runCatching {
        rawRequest(
            "/xlive/web-room/v2/index/getRoomPlayInfo",
            params,
            signed = true,
            base = liveBase,
            referer = referer,
        )
    }.getOrNull()
    pick(signed)
}

private fun parseLivePlayUrl(json: JSONObject): String? {
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
