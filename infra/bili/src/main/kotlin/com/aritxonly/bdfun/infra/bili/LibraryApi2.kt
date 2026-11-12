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

internal suspend fun BiliApiContext.enrichHistoryStats(items: List<HistoryItem>): List<HistoryItem> {
    if (items.isEmpty()) return items
    val missing = items.map { it.bvid }.distinct().filter { cachedVideoStat(it) == null }
    if (missing.isNotEmpty()) {
        val limiter = Semaphore(6)
        kotlinx.coroutines.coroutineScope {
            missing.forEach { bvid ->
                launch {
                    limiter.withPermit {
                        cancellableCatching {
                            val root = rawRequest(
                                "/x/web-interface/view",
                                mapOf("bvid" to bvid),
                            )
                            val stat = mapLibraryVideoStats(root)
                            if (stat != null) {
                                putVideoStat(bvid, stat)
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
    mapWatchLater(data)
}

internal suspend fun BiliApiContext.favoriteFolders(): List<FavoriteFolder> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/v3/fav/folder/created/list-all",
        mapOf("up_mid" to mid.toString()),
    )
    mapFavoriteFolders(data)
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
        mapFavoriteVideos(data)
    }

internal suspend fun BiliApiContext.relationStat(mid: Long): Pair<Long, Long> = withContext(Dispatchers.IO) {
    val data = dataRequest("/x/relation/stat", mapOf("vmid" to mid.toString()))
    mapRelationStat(data)
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
    mapFollowings(data)
}

/** 视频字幕列表：`/x/player/v2` 的 `subtitle.subtitles`（含 AI 字幕）。 */
