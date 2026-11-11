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

internal suspend fun BiliApiContext.upVideoCount(mid: Long): Long = withContext(Dispatchers.IO) {
    runCatching {
        val data = dataRequest("/x/space/navnum", mapOf("mid" to mid.toString()))
        data.l("video")
    }.getOrDefault(0L)
}

/** 当前同时观看人数：返回 (数字, 展示文案)。 */

internal suspend fun BiliApiContext.userLikes(mid: Long): Long = withContext(Dispatchers.IO) {
    runCatching {
        dataRequest("/x/space/upstat", mapOf("mid" to mid.toString())).l("likes")
    }.getOrDefault(0L)
}

internal suspend fun BiliApiContext.spaceAppPayload(mid: Long): JSONObject? = withContext(Dispatchers.IO) {
    runCatching {
        // app.bilibili.com 不吃网页 WBI 签名，必须按 App 规则加 appkey/ts/sign，
        // 否则只会返回 -400「请求错误」，头图永远空白。这里和 PiliPlus 的请求一致。
        val params = mutableMapOf(
            "build" to "8430300",
            "version" to "8.43.0",
            "mobi_app" to "android",
            "platform" to "android",
            "channel" to "master",
            "vmid" to mid.toString(),
            "c_locale" to "zh_CN",
            "s_locale" to "zh_CN",
            "statistics" to "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
        )
        signAppParams(params)
        dataRequest(
            "/x/v2/space",
            params,
            base = APP_API,
            userAgent = APP_UA,
            referer = "https://space.bilibili.com/$mid",
            headers = APP_HEADERS,
        )
    }.getOrNull()
}

internal fun BiliApiContext.spaceBannerCandidates(payload: JSONObject?, info: JSONObject): List<String> {
    val images = payload?.optJSONObject("images")
    val first = images?.optJSONObject("collection_top_simple")?.optJSONObject("top")
        ?.optJSONArray("result")?.optJSONObject(0)
    val collection = first?.optJSONObject("item")?.optJSONObject("image")?.s("default_image").orEmpty()
    val web = info.s("top_photo").ifBlank { info.optJSONObject("card")?.s("top_photo").orEmpty() }
        .ifBlank { info.optJSONObject("space")?.s("top_photo").orEmpty() }
    // The App endpoint can return its default pink dressing even when a custom web banner exists.
    return orderedSpaceBanners(collection, first?.s("cover").orEmpty(), web,
        images?.s("imgUrl").orEmpty(), images?.s("night_imgurl").orEmpty()).map(::image)
}

internal fun BiliApiContext.spaceBannerAlignment(payload: JSONObject?): Float {
    val item = payload?.optJSONObject("images")?.optJSONObject("collection_top_simple")
        ?.optJSONObject("top")?.optJSONArray("result")?.optJSONObject(0)?.optJSONObject("item")
    val image = item?.optJSONObject("image") ?: item?.optJSONObject("animation")
    return spaceBannerAlignment(image?.s("location").orEmpty(), image?.optDouble("height", 0.0) ?: 0.0)
}

/** 为 TA 充电的人数：使用 PiliPlus 同款 App 聚合接口的 elec.total。 */

internal suspend fun BiliApiContext.spaceChargeCount(mid: Long): Long = withContext(Dispatchers.IO) {
    val payload = spaceAppPayload(mid) ?: return@withContext 0L
    val elec = payload.optJSONObject("elec")
    elec?.l("total")?.takeIf { it > 0L }
        ?: elec?.optJSONArray("list")?.length()?.toLong()?.takeIf { it > 0L }
        ?: 0L
}

internal suspend fun BiliApiContext.space(mid: Long): SpaceInfo = withContext(Dispatchers.IO) {
    // 资料、关系、投稿、导航数互不依赖，并行拉取；页面总耗时从“四次相加”变成“最慢的一次”。
    val infoDeferred = async {
        dataRequest(
            "/x/space/wbi/acc/info",
            mapOf(
                "mid" to mid.toString(),
                "web_location" to "1550101",
                "dm_img_list" to "[]",
                "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEp",
                "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}",
            ),
            signed = true,
            referer = "https://space.bilibili.com/$mid",
        )
    }
    val statDeferred = async {
        runCatching {
            dataRequest("/x/relation/stat", mapOf("vmid" to mid.toString()))
        }.getOrNull()
    }
    val videosDeferred = async {
        runCatching {
            dataRequest(
                "/x/space/wbi/arc/search",
                mapOf(
                    "mid" to mid.toString(),
                    "pn" to "1",
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
        }.getOrNull()
    }
    val navDeferred = async {
        runCatching {
            dataRequest("/x/space/navnum", mapOf("mid" to mid.toString()))
        }.getOrNull()
    }
    // 头图、充电人数、IP 属地共用这一份 App 聚合数据。
    val appSpaceDeferred = async { spaceAppPayload(mid) }

    val info = infoDeferred.await()
    val stat = statDeferred.await()
    val videos = videosDeferred.await()
    val nav = navDeferred.await()
    val appSpace = appSpaceDeferred.await()
    val medal = info.optJSONObject("fans_medal")?.optJSONObject("medal")
    val official = info.optJSONObject("official")
    // 和 PiliPlus 一样只认 App 聚合接口的 card.vip。
    // PiliPlus 的 Vip.fromJson 是 status 优先、vipStatus 兜底，type 优先、vipType 兜底。
    val cardVip = appSpace?.optJSONObject("card")?.optJSONObject("vip")
    val vipStatus = cardVip?.optInt("status", 0)
        ?.takeIf { it > 0 }
        ?: (cardVip?.optInt("vipStatus", 0) ?: 0)
    val vipLabelObj = cardVip?.optJSONObject("label")
    val vipTheme = vipLabelObj?.s("label_theme").orEmpty()
    val vipType = cardVip?.optInt("type", 0)
        ?.takeIf { it > 0 }
        ?: (cardVip?.optInt("vipType", 0) ?: 0)
    val vipLabel = vipLabelObj?.s("text").orEmpty().ifBlank {
        when {
            vipTheme.contains("hundred", ignoreCase = true) -> "百年大会员"
            vipTheme.contains("ten_year", ignoreCase = true) -> "十年大会员"
            vipTheme.contains("ten", ignoreCase = true) -> "十年大会员"
            vipTheme.contains("annual", ignoreCase = true) -> "年度大会员"
            vipType == 2 -> "年度大会员"
            vipStatus == 1 -> "大会员"
            else -> ""
        }
    }
    val elec = appSpace?.optJSONObject("elec")
    val charge = elec?.l("total")?.takeIf { it > 0L }
        ?: elec?.optJSONArray("list")?.length()?.toLong()?.takeIf { it > 0L }
        ?: 0L
    val vlist = videos?.optJSONObject("list")?.optJSONArray("vlist").objects().orEmpty()
    SpaceInfo(
        profile = UserProfile(
            mid = info.l("mid"),
            name = info.s("name"),
            face = avatar(info.s("face"), 200),
            sign = info.s("sign"),
            level = info.optInt("level", 0),
            coins = 0.0,
            vip = vipStatus == 1,
            vipLabel = vipLabel,
            follower = stat?.l("follower") ?: 0,
            following = stat?.l("following") ?: 0,
            likes = info.optLong("likes", 0L).takeIf { it > 0L }
                ?: runCatching {
                    dataRequest("/x/space/upstat", mapOf("mid" to mid.toString())).l("likes")
                }.getOrDefault(0L).takeIf { it > 0L }
                ?: nav?.l("likes") ?: 0L,
            banner = spaceBannerCandidates(appSpace, info).firstOrNull().orEmpty(),
            bannerFallbacks = spaceBannerCandidates(appSpace, info).drop(1),
            bannerAlignmentY = spaceBannerAlignment(appSpace),
            officialTitle = official?.s("title").orEmpty(),
            officialDesc = official?.s("desc").orEmpty(),
            officialType = official?.optInt("type", 0) ?: 0,
            fansMedalName = medal?.s("medal_name").orEmpty(),
            fansMedalLevel = medal?.optInt("level", 0) ?: 0,
            fansMedalColor = medal?.optInt("medal_color", 0) ?: 0,
            chargingCount = charge,
            levelExp = info.optLong("level_exp", 0L),
            nextLevelExp = info.optLong("next_level_exp", 0L),
        ),
        videos = vlist.mapNotNull(this@space::parseSpaceVideo),
        videoCount = nav?.optInt("video", 0) ?: vlist.size,
        isFollowed = info.optBoolean("is_followed", false),
    )
}

internal suspend fun BiliApiContext.spaceSeasonsSeries(mid: Long): List<SpaceSeason> = withContext(Dispatchers.IO) {
    runCatching {
        val data = dataRequest(
            "/x/polymer/web-space/seasons_series_list",
            mapOf(
                "mid" to mid.toString(),
                "page_num" to "1",
                "page_size" to "10",
                "web_location" to "333.1387",
            ),
            referer = "https://space.bilibili.com/$mid",
        )
        val lists = data.optJSONObject("items_lists")
        val out = ArrayList<SpaceSeason>()
        lists?.optJSONArray("seasons_list")?.objects().orEmpty().forEach { item ->
            val meta = item.optJSONObject("meta") ?: return@forEach
            val id = meta.l("season_id")
            val title = meta.s("name").ifBlank { meta.s("title") }
            val cover = image(meta.s("cover"), 480)
            if (id <= 0L || title.isBlank() || cover.isBlank()) return@forEach
            out += SpaceSeason(
                id = id,
                title = title,
                cover = cover,
                total = meta.optInt("total", 0),
                isSeries = false,
            )
        }
        lists?.optJSONArray("series_list")?.objects().orEmpty().forEach { item ->
            val meta = item.optJSONObject("meta") ?: return@forEach
            val id = meta.l("series_id")
            val title = meta.s("name").ifBlank { meta.s("title") }
            val cover = image(meta.s("cover"), 480)
            if (id <= 0L || title.isBlank() || cover.isBlank()) return@forEach
            out += SpaceSeason(
                id = id,
                title = title,
                cover = cover,
                total = meta.optInt("total", 0),
                isSeries = true,
            )
        }
        out
    }.getOrDefault(emptyList())
}

/** 合集 / 系列的投稿列表。 */

internal suspend fun BiliApiContext.spaceCollectionArchives(
    mid: Long,
    collection: SpaceSeason,
): List<VideoItem> = withContext(Dispatchers.IO) {
    runCatching {
        val data = if (collection.isSeries) {
            dataRequest(
                "/x/series/archives",
                mapOf(
                    "mid" to mid.toString(),
                    "series_id" to collection.id.toString(),
                    "only_normal" to "true",
                    "sort" to "desc",
                    "pn" to "1",
                    "ps" to "30",
                    "web_location" to "333.1387",
                ),
                referer = "https://space.bilibili.com/$mid",
            )
        } else {
            dataRequest(
                "/x/polymer/web-space/seasons_archives_list",
                mapOf(
                    "mid" to mid.toString(),
                    "season_id" to collection.id.toString(),
                    "page_num" to "1",
                    "page_size" to "30",
                    "sort_reverse" to "false",
                    "web_location" to "333.1387",
                ),
                referer = "https://space.bilibili.com/$mid",
            )
        }
        data.optJSONArray("archives").objects().mapNotNull { archive ->
            val bvid = archive.s("bvid")
            if (bvid.isBlank()) return@mapNotNull null
            val stat = archive.optJSONObject("stat")
            VideoItem(
                bvid = bvid,
                aid = archive.l("aid"),
                title = archive.s("title"),
                cover = image(archive.s("pic"), 480),
                ownerName = "",
                ownerFace = "",
                ownerMid = mid,
                play = stat?.l("view") ?: archive.l("play"),
                danmaku = stat?.l("danmaku") ?: 0L,
                duration = archive.optInt("duration", 0),
                pubdate = archive.l("pubdate"),
                cid = archive.l("cid"),
            )
        }
    }.getOrDefault(emptyList())
}

/** 空间投稿（按发布时间，分页）。 */
