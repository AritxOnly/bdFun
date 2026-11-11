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
import com.aritxonly.bdfun.infra.bili.BiliParameters.API
import com.aritxonly.bdfun.infra.bili.BiliParameters.APP_API
import com.aritxonly.bdfun.infra.bili.BiliParameters.APP_UA
import com.aritxonly.bdfun.infra.bili.BiliParameters.APP
import com.aritxonly.bdfun.infra.bili.BiliParameters.APP_KEY
import com.aritxonly.bdfun.infra.bili.BiliParameters.APP_SEC
import com.aritxonly.bdfun.infra.bili.BiliParameters.TV_APP_KEY
import com.aritxonly.bdfun.infra.bili.BiliParameters.TV_APP_SEC
import com.aritxonly.bdfun.infra.bili.BiliParameters.APP_HEADERS
import com.aritxonly.bdfun.infra.bili.BiliParameters.PASSPORT
import com.aritxonly.bdfun.infra.bili.BiliParameters.VC
import com.aritxonly.bdfun.infra.bili.BiliParameters.UA
import com.aritxonly.bdfun.infra.bili.BiliParameters.encode
import com.aritxonly.bdfun.infra.bili.BiliParameters.md5
import com.aritxonly.bdfun.infra.bili.BiliParameters.image
import com.aritxonly.bdfun.infra.bili.BiliParameters.avatar
import com.aritxonly.bdfun.infra.bili.BiliParameters.cleanHtml
import com.aritxonly.bdfun.infra.bili.BiliParameters.aidToBvid
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseCountText
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseDurationText
import com.aritxonly.bdfun.infra.bili.BiliParameters.qualityLabel

internal fun BiliApiContext.jsonStrings(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        array.optString(index).takeIf { it.isNotBlank() }
    }
}

/** 给 CDN 设置测速用一个固定公开视频流。 */

internal suspend fun BiliApiContext.cdnSampleUrl(): String? = withContext(Dispatchers.IO) {
    val data = runCatching {
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
    val decompressed = runCatching {
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
    runCatching {
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
