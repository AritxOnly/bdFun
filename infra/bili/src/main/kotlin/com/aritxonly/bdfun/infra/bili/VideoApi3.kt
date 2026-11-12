package com.aritxonly.bdfun.infra.bili

import android.content.Context
import android.net.Uri
import android.text.Html
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.math.roundToInt
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.comment.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.infra.bili.mapper.*
import com.aritxonly.bdfun.infra.bili.BILI_API as API
import com.aritxonly.bdfun.infra.bili.BILI_APP_API as APP_API
import com.aritxonly.bdfun.infra.bili.BILI_APP_UA as APP_UA
import com.aritxonly.bdfun.infra.bili.BILI_APP as APP
import com.aritxonly.bdfun.infra.bili.BILI_APP_KEY as APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_APP_SEC as APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_KEY as TV_APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_SEC as TV_APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_APP_HEADERS as APP_HEADERS
import com.aritxonly.bdfun.infra.bili.BILI_PASSPORT as PASSPORT
import com.aritxonly.bdfun.infra.bili.BILI_VC as VC
import com.aritxonly.bdfun.infra.bili.BILI_UA as UA
import com.aritxonly.bdfun.infra.bili.encodeBiliParameter as encode
import com.aritxonly.bdfun.infra.bili.biliMd5 as md5
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.cleanBiliHtml as cleanHtml
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import com.aritxonly.bdfun.infra.bili.biliQualityLabel as qualityLabel

internal fun BiliApiContext.jsonStrings(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        array.optString(index).takeIf { it.isNotBlank() }
    }
}

/** 给 CDN 设置测速用一个固定公开视频流。 */

internal suspend fun BiliApiContext.cdnSampleUrl(): String? = withContext(Dispatchers.IO) {
    val data = cancellableCatching {
        dataRequest(
            "/x/player/playurl",
            mapOf(
                "bvid" to "BV1fK4y1t7hj",
                "cid" to "196018899",
                "qn" to "80",
                "fnval" to "4048",
                "fnver" to "0",
                "fourk" to "1",
            ),
        )
    }.getOrNull() ?: return@withContext null
    val rep = data.optJSONObject("dash")
        ?.optJSONArray("video")
        ?.objects()
        ?.firstOrNull()
        ?: return@withContext null
    val urls = buildList {
        add(rep.s("baseUrl").ifBlank { rep.s("base_url") })
        addAll(jsonStrings(rep.optJSONArray("backupUrl") ?: rep.optJSONArray("backup_url")))
    }.filter { it.isNotBlank() }
    // 优先用 /upgcxcode/ 形态的地址：mcdn 的 /vN/resource 地址换 host 会 403，不能用来测速。
    urls.firstOrNull { it.contains("/upgcxcode/") && !isMcdnResource(it) }
        ?: urls.firstOrNull { it.contains("/upgcxcode/") }
        ?: urls.firstOrNull()
}

internal fun BiliApiContext.decodeDanmakuBytes(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    val decompressed = cancellableCatching {
        when {
            bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte() -> {
                GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            }
            bytes.size > 2 && bytes[0] == 0x78.toByte() -> {
                InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            }
            bytes[0] == '<'.code.toByte() -> bytes
            else -> {
                java.util.zip.InflaterInputStream(
                    ByteArrayInputStream(bytes),
                    java.util.zip.Inflater(true),
                ).use { it.readBytes() }
            }
        }
    }.getOrNull()
    return String(decompressed ?: bytes, Charsets.UTF_8)
}

internal fun BiliApiContext.parseDanmakuXml(xml: String): List<DanmakuItem> {
    if (xml.isBlank()) return emptyList()
    val result = ArrayList<DanmakuItem>(512)
    cancellableCatching {
        val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(java.io.StringReader(xml))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && result.size < 4000) {
            if (event == XmlPullParser.START_TAG && parser.name == "d") {
                val p = parser.getAttributeValue(null, "p").orEmpty()
                val text = parser.nextText().orEmpty().trim()
                val parts = p.split(',')
                if (parts.size >= 4 && text.isNotBlank() && text.length <= 90) {
                    val seconds = parts[0].toFloatOrNull() ?: 0f
                    val mode = parts[1].toIntOrNull() ?: 1
                    if (mode == 1 || mode == 4 || mode == 5 || mode == 7 || mode == 8) {
                        result += DanmakuItem(
                            timeMs = (seconds * 1000).toLong(),
                            mode = mode,
                            color = parts[3].toIntOrNull() ?: 0xFFFFFF,
                            size = parts.getOrNull(2)?.toFloatOrNull() ?: 25f,
                            text = text,
                        )
                    }
                }
            }
            event = parser.next()
        }
    }
    return result.sortedBy { it.timeMs }
}
