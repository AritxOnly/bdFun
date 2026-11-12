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

internal suspend fun BiliApiContext.spaceVideos(mid: Long, page: Int): List<VideoItem> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/x/space/wbi/arc/search",
            mapOf(
                "mid" to mid.toString(),
                "pn" to page.toString(),
                "ps" to "30",
                "order" to "pubdate",
                "web_location" to "1550101",
                "dm_img_list" to "[]",
                "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEp",
                "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}",
            ),
            signed = true,
            referer = "https://space.bilibili.com/$mid",
        )
        data.optJSONObject("list")?.optJSONArray("vlist").objects()
            .mapNotNull(this@spaceVideos::parseSpaceVideo)
    }.getOrDefault(emptyList())
}

/** 空间图文投稿。 */

internal suspend fun BiliApiContext.spaceArticles(mid: Long, page: Int): List<SpaceArticle> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/x/space/wbi/article",
                mapOf(
                    "mid" to mid.toString(),
                    "pn" to page.toString(),
                    "ps" to "20",
                    "sort" to "publish_time",
                ),
                signed = true,
                referer = "https://space.bilibili.com/$mid",
            )
            data.optJSONArray("articles").objects().mapNotNull { article ->
                val id = article.l("id")
                if (id <= 0L) return@mapNotNull null
                val covers = article.optJSONArray("covers")
                val cover = when {
                    covers != null && covers.length() > 0 -> covers.optString(0)
                    else -> article.s("cover")
                }
                SpaceArticle(
                    id = id,
                    title = article.s("title"),
                    cover = image(cover, 480),
                    view = article.optJSONObject("stats")?.l("view")
                        ?: article.l("view"),
                    publishTime = article.l("publish_time"),
                )
            }
        }.getOrDefault(emptyList())
    }

/** 空间追番。 */

internal suspend fun BiliApiContext.spaceBangumi(mid: Long, page: Int): List<SpaceBangumi> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/x/space/bangumi",
                mapOf(
                    "mid" to mid.toString(),
                    "pn" to page.toString(),
                    "ps" to "30",
                ),
                referer = "https://space.bilibili.com/$mid",
            )
            data.optJSONArray("list").objects().mapNotNull { item ->
                val seasonId = item.l("season_id")
                val title = item.s("title")
                if (seasonId <= 0L || title.isBlank()) return@mapNotNull null
                SpaceBangumi(
                    seasonId = seasonId,
                    title = title,
                    cover = image(item.s("cover"), 480),
                    progress = item.s("progress"),
                    total = item.optInt("total_count", 0),
                )
            }
        }.getOrDefault(emptyList())
    }

/** UP 主小店：mall.bilibili.com 的 App 签名 POST 接口，先取第一页 8 个。 */

internal suspend fun BiliApiContext.spaceShop(mid: Long): List<SpaceShopItem> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val extras = mutableMapOf(
            "actionKey" to "appkey",
            "build" to "8430300",
            "mVersion" to "309",
            "mallVersion" to "8430300",
            "statistics" to
                "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
        )
        signAppParams(extras)
        val query = extras.entries.joinToString("&") { (key, value) ->
            "${android.net.Uri.encode(key)}=${android.net.Uri.encode(value)}"
        }
        val body = JSONObject()
            .put("from", "cps_productTab_$mid")
            .put("searchAfter", 0)
            .put("msource", "cps_productTab_$mid")
            .put("pageSize", 8)
            .put("upMid", mid.toString())
            .toString()
        val request = Request.Builder()
            .url("https://mall.bilibili.com/community-hub/small_shop/feed/tab/item?$query")
            .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("User-Agent", APP_UA)
            .header("Referer", "https://mall.bilibili.com/")
            .header("Origin", "https://mall.bilibili.com")
            .build()
        val root = http.executeOnce(request, tolerateEmptyBody = true)
        if (root.optInt("code", 0) != 0) return@cancellableCatching emptyList()
        val data = root.optJSONObject("data") ?: return@cancellableCatching emptyList()
        data.optJSONArray("data").objects().mapNotNull { item ->
            val title = item.s("title")
            if (title.isBlank()) return@mapNotNull null
            val coverObj = item.optJSONObject("cover")
            val priceObj = item.optJSONObject("netPrice")
            val price = if (priceObj != null) {
                buildString {
                    priceObj.s("pricePrefix").takeIf { it.isNotBlank() }?.let {
                        append(it); append(' ')
                    }
                    append(priceObj.s("priceSymbol"))
                    append(priceObj.s("netPrice"))
                }
            } else {
                ""
            }
            val labels = item.optJSONArray("belowLabels").objects()
                .mapNotNull { it.s("title").takeIf(String::isNotBlank) }
                .joinToString("|")
            SpaceShopItem(
                title = title,
                cover = image(coverObj?.s("url").orEmpty(), 480),
                price = price,
                source = labels.ifBlank { item.s("itemSourceName") },
                url = item.s("cardUrl"),
            )
        }
    }.getOrDefault(emptyList())
}

// ---------------------------------------------------------------- search

/**
 * 搜索词联想：PiliPlus 同款 s.search.bilibili.com/main/suggest。
 * 返回 term 作为真正提交的词，name 里可能带高亮 HTML，这里清洗掉。
 */

internal suspend fun BiliApiContext.userBrief(mid: Long): Pair<String, String>? = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/x/space/wbi/acc/info",
            mapOf("mid" to mid.toString()),
            signed = true,
        )
        val name = data.s("name")
        if (name.isBlank()) null else name to avatar(data.s("face"), 120)
    }.getOrNull()
}

internal suspend fun BiliApiContext.userIpLocation(mid: Long): String = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/x/space/wbi/acc/info",
            mapOf(
                "mid" to mid.toString(),
                "web_location" to "1550101",
            ),
            signed = true,
            referer = "https://space.bilibili.com/$mid",
        )
        data.s("ip_location")
    }.getOrDefault("")
}
