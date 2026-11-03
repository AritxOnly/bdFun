package com.aritxonly.bdfun

import java.net.HttpURLConnection
import java.net.URL

/** 从文本 / 分享链接里提取 B 站视频 BV 号；短链会跟随一次重定向。 */
object BiliLink {
    private val bvRegex = Regex("(?i)BV[0-9A-Za-z]{10}")
    private val shortRegex = Regex("(?i)https?://b23\\.tv/[0-9A-Za-z]+")
    private val videoRegex = Regex(
        "(?i)https?://(?:www\\.|m\\.)?bilibili\\.com/video/[0-9A-Za-z]+",
    )

    fun isVideoLink(text: String): Boolean =
        bvRegex.containsMatchIn(text) || shortRegex.containsMatchIn(text) ||
            videoRegex.containsMatchIn(text)

    /** 直接能看出 BV 就返回；b23 短链需要联网解析。 */
    fun resolve(text: String): String? {
        bvRegex.find(text)?.value?.let { return normalize(it) }
        val short = shortRegex.find(text)?.value ?: return null
        return runCatching {
            var url = URL(short)
            repeat(4) {
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                conn.connect()
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) return@repeat
                url = URL(url, location)
                bvRegex.find(url.toString())?.value?.let { return normalize(it) }
            }
            null
        }.getOrNull()
    }

    private fun normalize(bv: String): String = "BV" + bv.substring(2)
}
