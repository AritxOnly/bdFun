package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.VideoLinkPolicy
import java.net.HttpURLConnection
import java.net.URL

fun resolveVideoLink(text: String, open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }): String? {
    VideoLinkPolicy.directBvid(text)?.let { return it }
    val short = VideoLinkPolicy.shortUrl(text) ?: return null
    return runCatching {
        var url = URL(short)
        repeat(4) {
            val conn = open(url).apply {
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
            VideoLinkPolicy.directBvid(url.toString())?.let { return it }
        }
        null
    }.getOrNull()
}
