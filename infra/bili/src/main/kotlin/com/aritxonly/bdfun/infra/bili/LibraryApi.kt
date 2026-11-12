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

internal suspend fun BiliApiContext.favoriteFolderIds(aid: Long): Set<Long> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val root = rawRequest(
            "/x/v3/fav/resource/ids",
            mapOf("rid" to aid.toString(), "type" to "2"),
        )
        if (root.optInt("code", 0) != 0) return@cancellableCatching emptySet()
        val data = root.optJSONArray("data") ?: root.optJSONObject("data")?.optJSONArray("list")
        val out = mutableSetOf<Long>()
        data?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                when (item) {
                    is Number -> out.add(item.toLong())
                    is JSONObject -> {
                        val id = item.l("id").takeIf { it > 0 } ?: item.l("media_id")
                        if (id > 0) out.add(id)
                    }
                }
            }
        }
        out
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
        dataRequest("/x/relation", mapOf("fid" to mid.toString()))
            .optInt("attribute", 0)
    }.getOrNull()
}

/** HyperPiliPlus FollowHttp / FanHttp: server paging and owner privacy are preserved. */

internal suspend fun BiliApiContext.relationUsers(mid: Long, fans: Boolean, page: Int = 1, frequent: Boolean = false): RelationPage =
    withContext(Dispatchers.IO) {
        require(mid > 0 && page > 0)
        val data = dataRequest(if (fans) "/x/relation/fans" else "/x/relation/followings",
            relationListParams(mid, fans, page, frequent))
        val raw = data.optJSONArray("list").objects()
        val ids = raw.map { it.l("mid") }.filter { it > 0 }.distinct()
        // 'attribute' in another person's following list describes THEIR relationship.
        val relations = if (isLoggedIn && ids.isNotEmpty()) cancellableCatching {
            dataRequest("/x/relation/relations", mapOf("fids" to ids.joinToString(",")))
        }.getOrElse { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            null
        } else null
        val items = raw.mapNotNull { item ->
            val id = item.l("mid").takeIf { it > 0 } ?: return@mapNotNull null
            RelationUser(id, item.s("uname"), avatar(item.s("face"), 160), item.s("sign"),
                if (!isLoggedIn) 0 else relations?.optJSONObject(id.toString())?.optInt("attribute", 0))
        }
        val total = data.optInt("total", 0)
        RelationPage(items, total, relationNextPage(page, raw.size, total))
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
    val items = data.optJSONArray("list").objects().mapNotNull { item ->
        val history = item.optJSONObject("history") ?: return@mapNotNull null
        val bvid = history.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        val stat = item.optJSONObject("stat") ?: item.optJSONObject("videos")
        HistoryItem(
            bvid = bvid,
            title = item.s("title"),
            cover = image(item.s("cover"), 420),
            author = item.s("author_name"),
            viewedAt = item.l("view_at"),
            progress = item.optInt("progress", 0),
            duration = item.optInt("duration", 0),
            play = stat?.optInt("view", 0) ?: 0,
            danmaku = stat?.optInt("danmaku", 0) ?: 0,
            aid = history.l("oid"),
        )
    }
    val cursor = data.optJSONObject("cursor")
    val cursorHasMore = cursor?.optBoolean("has_more", false) == true ||
        cursor?.optInt("has_more", 0) == 1 ||
        data.optBoolean("has_more", false)
    log(
        "HISTORY",
        "page max=$maxCursor viewAt=$viewAtCursor items=${items.size} " +
            "cursorMax=${cursor?.l("max")} cursorViewAt=${cursor?.l("view_at")} " +
            "cursorHasMore=${cursor?.opt("has_more")}",
    )
    HistoryPage(
        items = items,
        max = cursor?.l("max") ?: 0L,
        viewAt = cursor?.l("view_at") ?: items.lastOrNull()?.viewedAt ?: 0L,
        business = cursor?.s("business").orEmpty(),
        hasMore = cursorHasMore || items.size >= 30,
    )
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
    val lastCid = data.l("last_play_cid")
    if (lastCid > 0 && lastCid != cid) return@withContext 0L
    val raw = data.l("last_play_time")
    if (raw <= 0) return@withContext 0L
    // 接口有的版本给秒、有的给毫秒：按视频时长判断一下，避免差 1000 倍。
    val ms = if (durationSec > 0 && raw <= durationSec * 2L) raw * 1000L else raw
    ms.coerceAtLeast(0L)
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
