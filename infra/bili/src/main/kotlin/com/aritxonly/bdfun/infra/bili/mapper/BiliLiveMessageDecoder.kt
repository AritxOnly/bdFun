package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.domain.LiveMessageDecoder
import com.aritxonly.bdfun.core.model.live.LiveChatKind
import com.aritxonly.bdfun.core.model.live.LiveChatMessage

import org.json.JSONObject

/**
 * 直播弹幕 JSON → 界面消息。
 *
 * 字段来源参考 BiliPai 的 LiveRealtimeMessagePolicy，只保留详情页需要的四类。
 */
class BiliLiveMessageDecoder : LiveMessageDecoder {

    override fun decode(raw: String, id: Long): LiveChatMessage? {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val cmd = json.optString("cmd").substringBefore(':')
        return when (cmd) {
            "DANMU_MSG" -> parseDanmaku(json, id)
            "SEND_GIFT", "COMBO_SEND" -> parseGift(json, id)
            "SUPER_CHAT_MESSAGE" -> parseSuperChat(json, id)
            "INTERACT_WORD" -> parseEnter(json, id)
            else -> null
        }
    }

    private fun parseDanmaku(json: JSONObject, id: Long): LiveChatMessage? {
        val info = json.optJSONArray("info") ?: return null
        // info[1] 是弹幕正文（字符串），info[2] 是用户数组，info[0] 是样式数组
        val text = info.optString(1)
        if (text.isBlank()) return null
        val user = info.optJSONArray(2)
        val uname = user?.optString(1).orEmpty()
        val meta = info.optJSONArray(0)
        val color = meta?.optInt(3, 0) ?: 0

        // 整条消息就是一张大表情
        val directEmote = meta?.optJSONObject(13)?.optString("url").orEmpty()

        // 文内表情：extra.emots 里是「token → 图片信息」
        val emotes = mutableMapOf<String, String>()
        val emoteHeights = mutableMapOf<String, Int>()
        val extra = meta?.optJSONObject(15)?.optString("extra").orEmpty()
        if (extra.isNotBlank()) {
            val emots = runCatching { JSONObject(extra).optJSONObject("emots") }.getOrNull()
            if (emots != null) {
                emots.keys().forEach { key ->
                    val item = emots.optJSONObject(key) ?: return@forEach
                    val url = item.optString("url")
                    if (url.isNotBlank()) {
                        emotes[key] = url
                        val h = item.optInt("height", 0)
                        if (h > 0) emoteHeights[key] = (h / 2).coerceIn(14, 40)
                    }
                }
            }
        }

        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.DANMAKU,
            uname = uname,
            text = text,
            nameColor = if (color > 0) color.toLong() else null,
            emoteUrl = directEmote.ifBlank { null },
            emotes = emotes,
            emoteHeights = emoteHeights,
        )
    }

    private fun parseGift(json: JSONObject, id: Long): LiveChatMessage? {
        val data = json.optJSONObject("data") ?: return null
        val uname = data.optString("uname")
        val gift = data.optString("giftName")
        val num = data.optInt("num", 1)
        if (uname.isBlank() || gift.isBlank()) return null
        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.GIFT,
            uname = uname,
            text = "赠送 $gift x$num",
        )
    }

    private fun parseSuperChat(json: JSONObject, id: Long): LiveChatMessage? {
        val data = json.optJSONObject("data") ?: return null
        val uname = data.optJSONObject("user_info")?.optString("uname").orEmpty()
        val message = data.optString("message")
        val price = data.optInt("price", 0)
        if (uname.isBlank() || message.isBlank()) return null
        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.SUPER_CHAT,
            uname = uname,
            text = message,
            amountText = if (price > 0) "￥$price" else null,
        )
    }

    private fun parseEnter(json: JSONObject, id: Long): LiveChatMessage? {
        val data = json.optJSONObject("data") ?: return null
        val uname = data.optString("uname")
        if (uname.isBlank()) return null
        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.SYSTEM,
            text = "欢迎 $uname 进入直播间",
        )
    }
}
