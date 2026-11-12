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

internal suspend fun BiliApiContext.favoriteFolderIds(aid: Long): Set<Long> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val root = rawRequest(
            "/x/v3/fav/resource/ids",
            mapOf("rid" to aid.toString(), "type" to "2"),
        )
        if (root.optInt("code", 0) != 0) return@cancellableCatching emptySet()
        mapFavoriteFolderIds(root)
    }.getOrDefault(emptySet())
}

/** 添加到收藏夹 / 从收藏夹移除。 */

internal suspend fun BiliApiContext.setFavorite(aid: Long, folderId: Long, add: Boolean): Boolean =
    withContext(Dispatchers.IO) {
        postRequest(
            "/x/v3/fav/resource/deal",
            mapOf(
                "rid" to aid.toString(),
                "type" to "2",
                "add_media_ids" to if (add) folderId.toString() else "",
                "del_media_ids" to if (add) "" else folderId.toString(),
                "csrf" to csrf,
                "platform" to "web",
            ),
        )
        true
    }

/** 一次性从多个收藏夹移除（逗号分隔），比逐个调用更稳。 */

internal suspend fun BiliApiContext.removeFavoriteAll(aid: Long, folderIds: Collection<Long>): Boolean =
    withContext(Dispatchers.IO) {
        if (folderIds.isEmpty()) return@withContext false
        postRequest(
            "/x/v3/fav/resource/deal",
            mapOf(
                "rid" to aid.toString(),
                "type" to "2",
                "add_media_ids" to "",
                "del_media_ids" to folderIds.joinToString(","),
                "csrf" to csrf,
                "platform" to "web",
            ),
        )
        true
    }

/** 添加到稍后再看。 */

internal suspend fun BiliApiContext.addWatchLater(aid: Long): Boolean = withContext(Dispatchers.IO) {
    cancellableCatching {
        postRequest(
            "/x/v2/history/toview/add",
            mapOf("aid" to aid.toString(), "csrf" to csrf),
        )
    }.isSuccess
}

/** 删除单条历史记录：kid 的官方格式是 archive_{aid}，不是 bvid。 */

internal suspend fun BiliApiContext.deleteHistory(aid: Long): Boolean = withContext(Dispatchers.IO) {
    if (aid <= 0) return@withContext false
    cancellableCatching {
        postRequest(
            "/x/v2/history/delete",
            mapOf("kid" to "archive_$aid", "csrf" to csrf),
        )
    }.isSuccess
}

/** 从稍后再看移除：接口要 aid。 */

internal suspend fun BiliApiContext.deleteWatchLater(aid: Long): Boolean = withContext(Dispatchers.IO) {
    cancellableCatching {
        postRequest(
            "/x/v2/history/toview/del",
            mapOf("aid" to aid.toString(), "csrf" to csrf),
        )
    }.isSuccess
}

internal suspend fun BiliApiContext.favoriteVideo(aid: Long, folderIds: String): Boolean = withContext(Dispatchers.IO) {
    postRequest(
        "/x/v3/fav/resource/deal",
        mapOf(
            "rid" to aid.toString(),
            "type" to "2",
            "add_media_ids" to folderIds,
            "del_media_ids" to "",
            "csrf" to csrf,
            "platform" to "web",
        ),
    )
    true
}

internal suspend fun BiliApiContext.followUser(mid: Long, follow: Boolean): Boolean = withContext(Dispatchers.IO) {
    postRequest(
        "/x/relation/modify",
        mapOf(
            "fid" to mid.toString(),
            "act" to if (follow) "1" else "2",
            "re_src" to "11",
            "csrf" to csrf,
        ),
    )
    true
}

/**
 * 当前登录用户与 [mid] 的关系。返回 attribute：
 * 0 未关注、2 已关注、6 互关、128 拉黑；未登录或失败返回 null。
 */

internal suspend fun BiliApiContext.relation(mid: Long): Int? = withContext(Dispatchers.IO) {
    if (!isLoggedIn || mid <= 0) return@withContext null
    cancellableCatching {
        mapRelationAttribute(dataRequest("/x/relation", mapOf("fid" to mid.toString())))
    }.getOrNull()
}

/** HyperPiliPlus FollowHttp / FanHttp: server paging and owner privacy are preserved. */

internal suspend fun BiliApiContext.relationUsers(mid: Long, fans: Boolean, page: Int = 1, frequent: Boolean = false): RelationPage =
    withContext(Dispatchers.IO) {
        require(mid > 0 && page > 0)
        val data = dataRequest(if (fans) "/x/relation/fans" else "/x/relation/followings",
            relationListParams(mid, fans, page, frequent))
        val ids = mapRelationTargetIds(data)
        // 'attribute' in another person's following list describes THEIR relationship.
        val relations = if (isLoggedIn && ids.isNotEmpty()) cancellableCatching {
            dataRequest("/x/relation/relations", mapOf("fids" to ids.joinToString(",")))
        }.getOrElse { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            null
        } else null
        mapRelationPage(data, relations, { isLoggedIn }, page)
    }

// ---------------------------------------------------------------- account

internal suspend fun BiliApiContext.history(): List<HistoryItem> = historyPage(0L, 0L, "").items

internal suspend fun BiliApiContext.historyPage(
    maxCursor: Long,
    viewAtCursor: Long,
    businessCursor: String = "",
): HistoryPage = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/web-interface/history/cursor",
        mapOf(
            "max" to maxCursor.toString(),
            "view_at" to viewAtCursor.toString(),
            "business" to businessCursor,
            "ps" to "20",
        ),
    )
    val mapped = mapHistoryPage(data)
    log("HISTORY", "page max=$maxCursor viewAt=$viewAtCursor items=${mapped.items.size} " + describeHistoryCursor(data))
    mapped
}

/**
 * 云端观看进度：/x/player/v2 会返回登录账号在该视频上的 last_play_time。
 * 这是 B 站自己的续播数据，换设备/重装后仍然有效。
 * last_play_cid 和当前 cid 不一致时说明上次看的是别的分 P，不套用。
 */

internal suspend fun BiliApiContext.cloudResumePositionMs(
    aid: Long,
    cid: Long,
    durationSec: Int = 0,
): Long = withContext(Dispatchers.IO) {
    if (aid <= 0 || cid <= 0 || !isLoggedIn) return@withContext 0L
    val data = cancellableCatching {
        dataRequest(
            "/x/player/v2",
            mapOf("aid" to aid.toString(), "cid" to cid.toString()),
        )
    }.getOrNull() ?: return@withContext 0L
    resolveCloudResumePositionMs(data.l("last_play_cid"), cid, data.l("last_play_time"), durationSec)
}

/**
 * 上报观看进度，官方「历史记录」才会收录本 App 播放的视频。
 * 对应网页端的 heartbeat 接口：type=3（视频）、dt=2（web）。
 */

internal suspend fun BiliApiContext.reportHistory(
    aid: Long,
    cid: Long,
    progressSec: Long,
): Boolean = withContext(Dispatchers.IO) {
    if (aid <= 0 || cid <= 0 || !isLoggedIn) return@withContext false
    cancellableCatching {
        postRequest(
            "/x/click-interface/web/heartbeat",
            mapOf(
                "aid" to aid.toString(),
                "cid" to cid.toString(),
                "mid" to mid.toString(),
                "csrf" to csrf,
                "played_time" to progressSec.coerceAtLeast(0).toString(),
                "real_played_time" to progressSec.coerceAtLeast(0).toString(),
                "realtime" to progressSec.coerceAtLeast(0).toString(),
                "start_ts" to (System.currentTimeMillis() / 1000).toString(),
                "type" to "3",
                "sub_type" to "0",
                "dt" to "2",
                "play_type" to "0",
            ),
        )
        true
    }.getOrDefault(false)
}

/**
 * 历史记录接口本身不带播放量/弹幕数，这里按 bvid 并发补取并缓存。
 * 并发压到 3，避免触发风控；同一 bvid 只请求一次。
 */
