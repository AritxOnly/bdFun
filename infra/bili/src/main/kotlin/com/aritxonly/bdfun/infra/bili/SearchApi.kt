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

internal suspend fun BiliApiContext.searchSuggest(term: String): List<String> = withContext(Dispatchers.IO) {
    if (term.isBlank()) return@withContext emptyList()
    cancellableCatching {
        val root = rawRequest(
            "/main/suggest",
            mapOf(
                "term" to term,
                "main_ver" to "v1",
                "highlight" to term,
            ),
            base = "https://s.search.bilibili.com",
        )
        root.optJSONObject("result")
            ?.optJSONArray("tag")
            .objects()
            .mapNotNull { item ->
                item.s("term")
                    .ifBlank { cleanHtml(item.s("name")) }
                    .takeIf { it.isNotBlank() }
            }
            .distinct()
            .take(8)
    }.getOrDefault(emptyList())
}

/** B 站热搜词。依次尝试「搜索发现」和「热搜词」，都失败时给一份兜底列表。 */

internal suspend fun BiliApiContext.hotKeywords(): List<String> = withContext(Dispatchers.IO) {
    // 1) 搜索发现：data.trending.list[].keyword
    val square = cancellableCatching {
        val data = dataRequest(
            "/x/web-interface/search/square",
            mapOf("limit" to "10"),
            signed = true,
        )
        val list = data.optJSONObject("trending")?.optJSONArray("list")
        val out = ArrayList<String>()
        if (list != null) {
            for (i in 0 until list.length()) {
                val word = list.optJSONObject(i)?.optString("keyword").orEmpty()
                if (word.isNotBlank()) out.add(word)
            }
        }
        out
    }.getOrDefault(emptyList())

    if (square.isNotEmpty()) return@withContext square

    // 2) 热搜词接口
    cancellableCatching {
        val root = rawRequest(
            path = "/main/hotword",
            params = emptyMap(),
            base = "https://s.search.bilibili.com",
        )
        val array = root.optJSONArray("data") ?: return@cancellableCatching emptyList<String>()
        val result = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val word = item.optString("keyword").ifBlank { item.optString("name") }
            if (word.isNotBlank()) result.add(word)
        }
        result
    }.getOrDefault(emptyList())
}

internal suspend fun BiliApiContext.search(
    keyword: String,
    page: Int = 1,
    /** totalrank 默认 / click 播放多 / pubdate 新发布 / dm 弹幕多 */
    order: String = "totalrank",
    /** 0 不限 / 1 0-10 分钟 / 2 10-30 / 3 30-60 / 4 60 以上 */
    duration: Int = 0,
    pubtimeBegin: Long? = null,
    pubtimeEnd: Long? = null,
): List<VideoItem> = withContext(Dispatchers.IO) {
    val params = LinkedHashMap<String, String>().apply {
        put("search_type", "video")
        put("keyword", keyword)
        put("page", page.toString())
        put("page_size", "20")
        put("order", order)
        if (duration in 1..4) put("duration", duration.toString())
        pubtimeBegin?.let { put("pubtime_begin_s", it.toString()) }
        pubtimeEnd?.let { put("pubtime_end_s", it.toString()) }
    }
    val data = dataRequest(
        "/x/web-interface/wbi/search/type",
        params,
        signed = true,
    )
    data.optJSONArray("result").objects().mapNotNull { item ->
        val bvid = item.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        VideoItem(
            bvid = bvid,
            aid = item.l("aid"),
            title = cleanHtml(item.s("title")),
            cover = image(item.s("pic"), 420),
            ownerName = cleanHtml(item.s("author")),
            ownerFace = "",
            ownerMid = item.l("mid"),
            play = item.s("play").toLongOrNull() ?: 0,
            danmaku = item.s("danmaku").toLongOrNull() ?: 0,
            duration = parseDurationText(item.s("duration")),
            pubdate = item.l("pubdate"),
        )
    }
}

/** 搜索用户（综合页 / 用户 Tab）。 */

internal suspend fun BiliApiContext.searchUsers(keyword: String, page: Int = 1): List<SearchUser> =
    withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/wbi/search/type",
            mapOf(
                "search_type" to "bili_user",
                "keyword" to keyword,
                "page" to page.toString(),
                "page_size" to "20",
            ),
            signed = true,
        )
        data.optJSONArray("result").objects().mapNotNull { item ->
            val mid = item.l("mid")
            if (mid <= 0L) return@mapNotNull null
            SearchUser(
                mid = mid,
                name = cleanHtml(item.s("uname")),
                face = image(item.s("upic"), 160),
                sign = cleanHtml(item.s("usign")),
                fans = item.s("fans").toLongOrNull() ?: 0,
                videos = item.s("videos").toLongOrNull() ?: 0,
                level = item.optInt("level", 0),
            )
        }
    }

internal suspend fun BiliApiContext.hotSearch(): List<String> = withContext(Dispatchers.IO) {
    val data = cancellableCatching {
        dataRequest("/x/web-interface/search/square", mapOf("limit" to "10"))
    }.getOrNull() ?: return@withContext emptyList()
    data.optJSONObject("trending")?.optJSONArray("list").objects()
        ?.mapNotNull { it.s("keyword").takeIf(String::isNotBlank) }
        .orEmpty()
}
