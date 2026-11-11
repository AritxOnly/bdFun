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

internal suspend fun BiliApiContext.enrichHistoryStats(items: List<HistoryItem>): List<HistoryItem> {
    if (items.isEmpty()) return items
    val missing = items.map { it.bvid }.distinct().filter { cachedVideoStat(it) == null }
    if (missing.isNotEmpty()) {
        val limiter = Semaphore(6)
        kotlinx.coroutines.coroutineScope {
            missing.forEach { bvid ->
                launch {
                    limiter.withPermit {
                        runCatching {
                            val root = rawRequest(
                                "/x/web-interface/view",
                                mapOf("bvid" to bvid),
                            )
                            val stat = root.optJSONObject("data")?.optJSONObject("stat")
                            if (stat != null) {
                                putVideoStat(bvid, stat.l("view") to stat.l("danmaku"))
                            }
                        }
                    }
                }
            }
        }
    }
    return items.map { item ->
        val cached = cachedVideoStat(item.bvid)
        if (cached == null) {
            item
        } else {
            item.copy(play = cached.first.toInt(), danmaku = cached.second.toInt())
        }
    }
}

/** 稍后再看：/x/v2/history/toview，字段结构与历史记录接近。 */

internal suspend fun BiliApiContext.watchLater(): List<HistoryItem> = withContext(Dispatchers.IO) {
    val data = dataRequest("/x/v2/history/toview", emptyMap())
    data.optJSONArray("list").objects().mapNotNull { item ->
        val bvid = item.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        val stat = item.optJSONObject("stat")
        HistoryItem(
            bvid = bvid,
            title = item.s("title"),
            cover = image(item.s("pic"), 420),
            author = item.optJSONObject("owner")?.s("name").orEmpty(),
            viewedAt = item.l("add_at"),
            progress = item.optInt("progress", 0),
            duration = item.optInt("duration", 0),
            play = stat?.optInt("view", 0) ?: 0,
            danmaku = stat?.optInt("danmaku", 0) ?: 0,
            aid = item.l("aid"),
        )
    }
}

internal suspend fun BiliApiContext.favoriteFolders(): List<FavoriteFolder> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/v3/fav/folder/created/list-all",
        mapOf("up_mid" to mid.toString()),
    )
    data.optJSONArray("list").objects().map { folder ->
        FavoriteFolder(
            id = folder.l("id"),
            title = folder.s("title"),
            count = folder.optInt("media_count", 0),
            cover = image(folder.s("cover"), 300),
        )
    }
}

internal suspend fun BiliApiContext.favoriteVideos(folderId: Long): List<VideoItem> =
    favoriteVideosPage(folderId, 1).first

internal suspend fun BiliApiContext.favoriteVideosPage(folderId: Long, page: Int): Pair<List<VideoItem>, Boolean> =
    withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/v3/fav/resource/list",
            mapOf(
                "media_id" to folderId.toString(),
                "pn" to page.toString(),
                "ps" to "20",
                "order" to "mtime",
                "type" to "0",
                "tid" to "0",
                "platform" to "web",
            ),
        )
        val medias = data.optJSONArray("medias").objects()
        val videos = medias.mapNotNull { media ->
            val bvid = media.s("bvid")
            if (bvid.isBlank()) return@mapNotNull null
            val upper = media.optJSONObject("upper")
            val cnt = media.optJSONObject("cnt_info")
            VideoItem(
                bvid = bvid,
                aid = media.l("id"),
                title = media.s("title"),
                cover = image(media.s("cover"), 420),
                ownerName = upper?.s("name").orEmpty(),
                ownerFace = "",
                ownerMid = upper?.l("mid") ?: 0,
                play = cnt?.l("play") ?: 0,
                danmaku = cnt?.l("danmaku") ?: 0,
                duration = media.optInt("duration", 0),
                pubdate = media.l("pubtime"),
            )
        }
        videos to data.optBoolean("has_more", medias.size >= 40)
    }

internal suspend fun BiliApiContext.relationStat(mid: Long): Pair<Long, Long> = withContext(Dispatchers.IO) {
    val data = dataRequest("/x/relation/stat", mapOf("vmid" to mid.toString()))
    (data.l("following") to data.l("follower"))
}

/** 用户获赞总数（官方空间统计）。 */

internal suspend fun BiliApiContext.followings(): List<FollowedUser> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/relation/followings",
        mapOf(
            "vmid" to mid.toString(),
            "pn" to "1",
            "ps" to "50",
            "order" to "desc",
            "order_type" to "attention",
        ),
    )
    data.optJSONArray("list").objects().map { user ->
        FollowedUser(
            mid = user.l("mid"),
            name = user.s("uname"),
            face = avatar(user.s("face"), 160),
            sign = user.s("sign"),
        )
    }
}

/** 视频字幕列表：`/x/player/v2` 的 `subtitle.subtitles`（含 AI 字幕）。 */
