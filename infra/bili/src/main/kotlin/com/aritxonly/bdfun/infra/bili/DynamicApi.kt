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

internal suspend fun BiliApiContext.userDynamics(mid: Long, offset: String = ""): DynamicPage =
    withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/polymer/web-dynamic/v1/feed/space",
            mapOf(
                "host_mid" to mid.toString(),
                "offset" to offset,
                "page" to "1",
                "web_location" to "333.999",
                "features" to "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote",
            ),
            referer = "https://space.bilibili.com/$mid/dynamic",
        )
        DynamicPage(
            items = data.optJSONArray("items").objects().mapNotNull { parseDynamicItem(it) },
            offset = data.s("offset"),
            hasMore = data.optBoolean("has_more", false),
        )
    }

/** UP 主投稿视频数。 */

internal suspend fun BiliApiContext.dynamics(): List<DynamicItem> = dynamicPage("all").items

/** Hyper-PiliPlus uses the same feed for categories and a selected UP. */

internal suspend fun BiliApiContext.dynamicPage(type: String, offset: String = "", hostMid: Long? = null): DynamicPage =
    withContext(Dispatchers.IO) {
        val data = try {
            dataRequest(DynamicFeedContract.feedPath, DynamicFeedContract.feedParams(type, offset, hostMid))
        } catch (error: BiliException) {
            if (error.code == 4101132) return@withContext DynamicPage(emptyList(), "", false)
            throw error
        }
        DynamicPage(
            items = data.optJSONArray("items").objects().mapNotNull { parseDynamicItem(it) },
            offset = data.s("offset"),
            hasMore = data.optBoolean("has_more", false),
        )
    }

internal suspend fun BiliApiContext.dynamicPortal(): DynamicPortal = withContext(Dispatchers.IO) {
    val data = dataRequest(DynamicFeedContract.portalPath, DynamicFeedContract.portalParams)
    val live = data.optJSONObject("live_users")
    DynamicPortal(
        upList = parseDynamicUpPage(data.optJSONObject("up_list")),
        liveUsers = live?.optJSONArray("items").objects().mapNotNull {
            val roomId = it.l("room_id")
            if (roomId <= 0) null else DynamicLiveUser(
                mid = it.l("mid"), name = it.s("uname"), face = avatar(it.s("face"), 120),
                roomId = roomId, title = it.s("title"),
            )
        },
        liveCount = live?.optInt("count", 0) ?: 0,
    )
}

internal suspend fun BiliApiContext.dynamicUpList(offset: String): DynamicUpPage = withContext(Dispatchers.IO) {
    parseDynamicUpPage(dataRequest(DynamicFeedContract.upListPath, DynamicFeedContract.upListParams(offset)))
}

internal fun BiliApiContext.parseDynamicUpPage(data: JSONObject?): DynamicUpPage = DynamicUpPage(
    items = data?.optJSONArray("items").objects().mapNotNull {
        val mid = it.l("mid")
        if (mid <= 0) null else DynamicAuthor(mid, it.s("uname"), avatar(it.s("face"), 120), it.optBoolean("has_update"))
    }.distinctBy { it.mid },
    offset = data?.s("offset").orEmpty(),
    hasMore = data?.optBoolean("has_more", false) ?: false,
)

internal fun BiliApiContext.parseDynamicItem(item: JSONObject, allowOrig: Boolean = true): DynamicItem? {
    val modules = item.optJSONObject("modules") ?: return null
    val author = modules.optJSONObject("module_author") ?: return null
    val dynamic = modules.optJSONObject("module_dynamic")
    val desc = dynamic?.optJSONObject("desc")?.s("text").orEmpty()
    val major = dynamic?.optJSONObject("major")
    val archive = major?.optJSONObject("archive")
    val opus = major?.optJSONObject("opus")
    val article = major?.optJSONObject("article")
    val common = major?.optJSONObject("common")
    val pgc = major?.optJSONObject("pgc")
    val live = major?.optJSONObject("live")
    val liveRcmd = major?.optJSONObject("live_rcmd")
        ?: major?.optJSONObject("subscription_new")?.optJSONObject("live_rcmd")
    val liveInfo = liveRcmd?.s("content")?.takeIf { it.isNotBlank() }
        ?.let { runCatching { JSONObject(it).optJSONObject("live_play_info") }.getOrNull() }
    val liveRecommendation = liveInfo?.let {
        DynamicLiveRecommendation(
            roomId = it.l("room_id"), title = it.s("title"), cover = image(it.s("cover"), 720),
            live = it.optInt("live_status") == 1, areaName = it.s("area_name"),
            watched = it.optJSONObject("watched_show")?.s("text_large").orEmpty(),
        )
    }
    val medialist = major?.optJSONObject("medialist")
    val courses = major?.optJSONObject("courses")
    val music = major?.optJSONObject("music")
    val ugcSeason = major?.optJSONObject("ugc_season")
    val video = archive?.let {
        VideoItem(
            bvid = it.s("bvid"),
            aid = it.l("aid"),
            title = it.s("title"),
            cover = image(it.s("cover"), 480),
            ownerName = author.s("name"),
            ownerFace = avatar(author.s("face"), 120),
            ownerMid = author.l("mid"),
            play = parseCountText(it.optJSONObject("stat")?.optString("play").orEmpty()),
            danmaku = parseCountText(it.optJSONObject("stat")?.optString("danmaku").orEmpty()),
            duration = parseDurationText(it.s("duration_text")),
            pubdate = 0,
            cid = 0,
        )
    } ?: ugcSeason?.let {
        VideoItem(
            bvid = it.s("bvid"),
            aid = it.l("aid"),
            title = it.s("title"),
            cover = image(it.s("cover"), 480),
            ownerName = author.s("name"),
            ownerFace = avatar(author.s("face"), 120),
            ownerMid = author.l("mid"),
            play = parseCountText(it.optJSONObject("stat")?.optString("play").orEmpty()),
            danmaku = parseCountText(it.optJSONObject("stat")?.optString("danmaku").orEmpty()),
            duration = parseDurationText(it.s("duration_text")),
            pubdate = 0,
            cid = 0,
        )
    }
    // 图文 opus：feed 在 itemOpusStyle 下把标题 / 正文放在 major.opus 里，
    // 只读 module_dynamic.desc 会出现“很多内容显示不出来”。
    val opusSummary = opus?.optJSONObject("summary")?.s("text").orEmpty()
    val title = opus?.s("title").orEmpty()
        .ifBlank { article?.s("title").orEmpty() }
        .ifBlank { common?.s("title").orEmpty() }
        .ifBlank { live?.s("title").orEmpty() }
        .ifBlank { medialist?.s("title").orEmpty() }
        .ifBlank { courses?.s("title").orEmpty() }
        .ifBlank { music?.s("title").orEmpty() }
    val text = desc
        .ifBlank { opusSummary }
        .ifBlank { article?.s("desc").orEmpty() }
        .ifBlank { common?.s("desc").orEmpty() }
        .ifBlank { common?.s("summary").orEmpty() }
        .ifBlank { pgc?.s("desc").orEmpty() }
        .ifBlank { medialist?.s("desc").orEmpty() }
        .ifBlank { music?.s("desc").orEmpty() }
    val images = buildList {
        major?.optJSONObject("draw")?.optJSONArray("items")?.objects()?.forEach {
            it.s("src").takeIf(String::isNotBlank)?.let { src -> add(image(src, 720)) }
        }
        opus?.optJSONArray("pics")?.objects()?.forEach {
            it.s("url").takeIf(String::isNotBlank)?.let { url -> add(image(url, 720)) }
        }
        article?.optJSONArray("covers")?.let { covers ->
            for (i in 0 until covers.length()) {
                val value = covers.opt(i)
                val url = when (value) {
                    is String -> value
                    is JSONObject -> value.s("url")
                    else -> ""
                }
                if (url.isNotBlank()) add(image(url, 720))
            }
        }
        common?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        live?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        medialist?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        courses?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        music?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
    }.filter { it.isNotBlank() }.distinct()
    val stat = modules.optJSONObject("module_stat")
    val basic = item.optJSONObject("basic")
    val orig = if (allowOrig) {
        item.optJSONObject("orig")?.let { parseDynamicItem(it, allowOrig = false) }
    } else {
        null
    }
    return DynamicItem(
        id = item.s("id_str"),
        name = author.s("name"),
        face = avatar(author.s("face"), 120),
        mid = author.l("mid"),
        time = author.s("pub_time"),
        text = text,
        images = images,
        video = video,
        like = stat?.optJSONObject("like")?.optInt("count", 0) ?: 0,
        comment = stat?.optJSONObject("comment")?.optInt("count", 0) ?: 0,
        forward = stat?.optJSONObject("forward")?.optInt("count", 0) ?: 0,
        liked = stat?.optJSONObject("like")?.optBoolean("status", false) ?: false,
        commentOid = basic?.s("comment_id_str").orEmpty()
            .ifBlank { item.s("id_str") },
        commentType = (basic?.optInt("comment_type", 17) ?: 17)
            .takeIf { it > 0 } ?: 17,
        title = title,
        orig = orig,
        authorVip = author.optJSONObject("vip")?.optInt("vipType", 0) == 2,
        isCollection = !ugcSeason?.s("bvid").isNullOrBlank(),
        pgcCover = pgc?.s("cover").orEmpty().takeIf { it.isNotBlank() }
            ?.let { image(it, 720) }.orEmpty(),
        pgcTitle = pgc?.s("title").orEmpty(),
        pgcLabel = when (pgc?.optInt("sub_type", 0)) {
            3 -> "纪录片"
            1 -> "番剧"
            2 -> "电影"
            4 -> "国创"
            5 -> "电视剧"
            else -> "剧集"
        },
        moduleTag = modules.optJSONObject("module_tag")?.s("text").orEmpty(),
        liveRecommendation = liveRecommendation,
        ipLocation = author.s("pub_location_text")
            .ifBlank { author.s("pub_location") }
            .ifBlank { author.s("ip_location") }
            .ifBlank { author.s("location") },
    )
}

/**
 * 动态详情官方接口：feed 只给摘要时，打开详情用 detail 接口拿完整的
 * opus 正文 / 图片 / 转发内容。失败返回 null，由调用方回退到列表项。
 */

internal suspend fun BiliApiContext.dynamicDetail(id: String): DynamicItem? = withContext(Dispatchers.IO) {
    runCatching {
        val data = dataRequest(
            "/x/polymer/web-dynamic/v1/detail",
            mapOf(
                "id" to id,
                "dynamic_id" to id,
                "platform" to "web",
                "gaia_source" to "main_web",
                "web_location" to "333.1368",
                "features" to "itemOpusStyle,opusBigCover,onlyfansVote," +
                    "endFooterHidden,decorationCard,onlyfansAssetsV2,ugcDelete," +
                    "onlyfansQaCard,commentsNewVersion",
            ),
        )
        data.optJSONObject("item")?.let { parseDynamicItem(it) }
    }.getOrNull()
}

/** 动态点赞 / 取消。新接口要求 csrf 放 URL、正文是 JSON（form 会 4100001）。 */

internal suspend fun BiliApiContext.likeDynamic(id: String, like: Boolean): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        ensureBuvid()
        val jsonBody = JSONObject()
            .put("dyn_id_str", id)
            .put("up", if (like) 1 else 2)
            .toString()
        val request = Request.Builder()
            .url("$API/x/dynamic/feed/dyn/thumb?csrf=$csrf")
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .header("Origin", "https://www.bilibili.com")
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        http.await(request).use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw BiliException(-1, "服务器返回为空")
            val json = JSONObject(body)
            val code = json.optInt("code", 0)
            if (code != 0) {
                throw BiliException(code, apiMessage(code, json.optString("message")))
            }
        }
        true
    }.onFailure {
        if (debug) {
            android.util.Log.w("BiliApi", "likeDynamic failed id=$id like=$like", it)
        }
    }.getOrDefault(false)
}

/** 动态详情评论（type=17）。 */
