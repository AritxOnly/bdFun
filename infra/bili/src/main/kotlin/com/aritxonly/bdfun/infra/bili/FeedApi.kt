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

internal suspend fun BiliApiContext.recommend(freshIndex: Int = 1, web: Boolean = true): List<VideoItem> = withContext(Dispatchers.IO) {
    var primaryError: Exception? = null
    val primary = try {
        if (web) webRecommend(freshIndex) else appRecommend(freshIndex)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        primaryError = e
        emptyList()
    }
    if (primary.isNotEmpty()) {
        log(
            "FEED",
            "source=${if (web) "web" else "app"} count=${primary.size} " +
                "first=${primary.firstOrNull()?.bvid}",
        )
        return@withContext primary
    }

    // 纯网络错误说明整个网络都不通，立刻报错，避免再用另一个接口白等一轮。
    val netError = primaryError
    if (netError is BiliException && netError.code == -1) throw netError

    // Web 推荐失败/为空时用 App 推荐兜底，反之亦然；总比整页空白强。
    val fallback = try {
        if (web) appRecommend(freshIndex) else webRecommend(freshIndex)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }
    if (fallback.isNotEmpty()) {
        log(
            "FEED",
            "source=${if (web) "web->app-fallback" else "app->web-fallback"} " +
                "count=${fallback.size} first=${fallback.firstOrNull()?.bvid}",
        )
        return@withContext fallback
    }
    if (primaryError != null) throw primaryError
    emptyList()
}

internal suspend fun BiliApiContext.webRecommend(freshIndex: Int): List<VideoItem> {
    val data = dataRequest(
        "/x/web-interface/wbi/index/top/feed/rcmd",
        mapOf(
            "web_location" to "1430650",
            "y_num" to "5",
            "fresh_type" to "4",
            "feed_version" to "V8",
            "fresh_idx_1h" to freshIndex.toString(),
            "fetch_row" to "1",
            "fresh_idx" to freshIndex.toString(),
            "brush" to "0",
            "homepage_ver" to "1",
            "ps" to "12",
        ),
        signed = true,
    )
    return data.optJSONArray("item").objects().mapNotNull(this@webRecommend::parseFeedVideo)
}

internal suspend fun BiliApiContext.appRecommend(freshIndex: Int): List<VideoItem> {
    // App 推荐域名是 app.bilibili.com；用 api.bilibili.com 会 404，随后悄悄退回 Web 推荐。
    val data = dataRequest(
        "/x/v2/feed/index",
        mapOf(
            "build" to "7880300",
            "mobi_app" to "android",
            "platform" to "android",
            "idx" to freshIndex.toString(),
            "login_event" to "0",
            "network" to "wifi",
            "style" to "2",
            "ps" to "12",
        ),
        base = APP,
    )
    val out = data.optJSONArray("items").objects().mapNotNull { raw ->
        val args = raw.optJSONObject("args")
            ?: runCatching { JSONObject(raw.optString("args")) }.getOrNull()
            ?: JSONObject()
        val player = raw.optJSONObject("player_args")
        val cardGoto = raw.s("card_goto")
        val goto = raw.s("goto")
        // App 推荐里混着直播、广告、横幅、游戏卡和竖屏短视频；
        // 竖屏卡片的 aid 是 12 位以上，标准播放接口打不开，这里只要普通稿件。
        val isVideoCard = cardGoto == "av" || goto == "av" ||
            (cardGoto.isEmpty() && goto.isEmpty() &&
                raw.s("card_type").startsWith("small_cover"))
        if (!isVideoCard) return@mapNotNull null
        val aid = args.l("aid")
            .takeIf { it > 0L }
            ?: player?.l("aid")?.takeIf { it > 0L }
            ?: raw.s("param").toLongOrNull()
            ?: 0L
        // av↔bv 公式只支持 32 位 aid；超出的都是竖屏/广告卡片。
        if (aid !in 1L..2_147_483_647L) return@mapNotNull null
        val directBvid = raw.s("bvid").ifBlank { args.s("bvid") }
            .ifBlank { player?.s("bvid").orEmpty() }
        val bvid = directBvid.ifBlank { aidToBvid(aid) }
        if (bvid.isBlank()) return@mapNotNull null
        VideoItem(
            bvid = bvid,
            aid = aid,
            title = cleanHtml(raw.s("title")),
            cover = image(raw.s("cover"), 480),
            ownerName = args.s("up_name").ifBlank { args.s("name") },
            ownerFace = avatar(args.s("up_face").ifBlank { args.s("face") }, 96),
            ownerMid = args.l("up_id").takeIf { it > 0 } ?: args.l("mid"),
            play = parseCountText(raw.s("cover_left_text_1")),
            danmaku = parseCountText(raw.s("cover_left_text_2")),
            duration = player?.optInt("duration", 0)
                ?.takeIf { it > 0 }
                ?: parseDurationText(raw.s("cover_right_text")),
            pubdate = 0,
            cid = player?.l("cid")?.takeIf { it > 0 } ?: 0,
            categoryName = args.s("tname").ifBlank { raw.s("tname") },
            appRecommendation = true,
            dislikeReasons = (raw.optJSONArray("three_point_v2").objects()
                .filter { it.s("type") == "dislike" }.flatMap { it.optJSONArray("reasons").objects() } +
                raw.optJSONObject("three_point")?.optJSONArray("dislike_reasons").objects())
                .map { RecommendationReason(it.l("id"), it.s("name")) }.distinctBy { it.id },
            isFollowed = args.optBoolean("is_followed", false) ||
                raw.optBoolean("is_followed", false),
        )
    }
    val rawCount = data.optJSONArray("items")?.length() ?: 0
    log("FEED", "app-raw=$rawCount kept=${out.size}")
    return out
}

internal suspend fun BiliApiContext.dislikeRecommendation(video: VideoItem, reason: RecommendationReason) {
    if (video.appRecommendation) {
        check(cookieJar.value("access_key").isNotBlank()) { "请重新扫码登录以提交 App 推荐反馈" }
        val params = mutableMapOf("goto" to "av", "id" to video.aid.toString(),
            "reason_id" to reason.id.toString(), "build" to "1", "mobi_app" to "android")
        signAppParams(params)
        dataRequest("/x/feed/dislike", params, base = APP)
    } else {
        postRequest("/x/web-interface/feedback/dislike", mapOf(
            "app_id" to "100", "platform" to "5", "from_spmid" to "", "spmid" to "333.1007.0.0",
            "goto" to "av", "id" to video.aid.toString(), "mid" to video.ownerMid.toString(),
            "track_id" to video.recommendationTrackId, "feedback_page" to "1",
            "reason_id" to reason.id.toString(), "csrf" to csrf), tolerateEmptyBody = false)
    }
}

internal suspend fun BiliApiContext.popular(page: Int = 1): List<VideoItem> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/web-interface/popular",
        mapOf("pn" to page.toString(), "ps" to "20"),
    )
    data.optJSONArray("list").objects().mapNotNull(this@popular::parseFeedVideo)
}

internal suspend fun BiliApiContext.ranking(rid: Int = 0): List<VideoItem> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/web-interface/ranking/v2",
        mapOf("rid" to rid.toString(), "type" to "all"),
    )
    data.optJSONArray("list").objects().mapNotNull(this@ranking::parseFeedVideo)
}

// ---------------------------------------------------------------- video

internal fun BiliApiContext.parseFeedVideo(item: JSONObject): VideoItem? =
    FeedVideoMapper.read(item)?.let { FeedVideoMapper.map(it, ::cleanHtml) }
