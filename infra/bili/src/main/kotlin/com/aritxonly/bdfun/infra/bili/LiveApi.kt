package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.live.LiveRoomInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.aritxonly.bdfun.infra.bili.mapper.*

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
    mapLiveRoomInfo(json, roomId)
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
        val id = readLiveHotRoomId(json)
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
    mapLivePortraitBackground(json)
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
    mapLiveQualities(json)
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
    mapLiveEmoticons(json)
}

/**
 * 弹幕服务器：token + wss 地址列表。
 */
internal suspend fun BiliApiContext.liveDanmuInfo(roomId: Long): Pair<String, List<String>>? = withContext(Dispatchers.IO) {
    val realRoomId = runCatching {
        readLiveRoomId(rawRequest(
            "/room/v1/Room/room_init",
            mapOf("id" to roomId.toString()),
            base = liveBase,
            referer = "https://live.bilibili.com/$roomId",
        ))
    }.getOrDefault(0L).takeIf { it > 0L } ?: roomId

    val referer = "https://live.bilibili.com/$realRoomId"
    val params = mapOf(
        "id" to realRoomId.toString(),
        "type" to "0",
        "web_location" to "444.8",
    )

    // 和 BiliPai 一致：这个接口必须带 WBI 签名（否则 -352），失败就刷新密钥重试一次
    suspend fun signed(): JSONObject? = runCatching {
        rawRequest(
            "/xlive/web-room/v1/index/getDanmuInfo", params,
            signed = true, base = liveBase, referer = referer,
        )
    }.getOrNull()

    mapLiveDanmuInfo(signed())?.let { return@withContext it }
    // 刷新 WBI 密钥后重试
    wbiSigner.clear()
    mapLiveDanmuInfo(signed())
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
