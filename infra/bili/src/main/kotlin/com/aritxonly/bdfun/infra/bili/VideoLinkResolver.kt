package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.shortVideoUrl
import com.aritxonly.bdfun.core.domain.directVideoBvid
import java.net.HttpURLConnection
import java.net.URL

fun resolveVideoLink(text: String, open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }): String? {
    directVideoBvid(text)?.let { return it }
    val short = shortVideoUrl(text) ?: return null
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
            directVideoBvid(url.toString())?.let { return it }
        }
        null
    }.getOrNull()
}
