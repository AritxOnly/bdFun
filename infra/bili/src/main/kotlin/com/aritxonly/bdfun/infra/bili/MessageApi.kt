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

internal suspend fun BiliApiContext.messages(type: Int, page: Int = 1): List<MessageItem> = messagesPage(type).items

internal suspend fun BiliApiContext.messagesPage(type: Int, cursor: MessageCursor? = null): MessageFeedPage = withContext(Dispatchers.IO) {
    if (type == 3) return@withContext systemMessages(cursor)
    val path = when (type) {
        0 -> "/x/msgfeed/reply"
        1 -> "/x/msgfeed/at"
        2 -> "/x/msgfeed/like"
        else -> error("Unknown message feed")
    }
    val params = messageFeedParams(type, cursor)
    val data = dataRequest(path, params)
    // 收到的赞返回的是 { latest: { items }, total: { items } } 两个分桶，
    // 不是顶层的 items；原来只读顶层，所以这个页面一直是空的。
    val rawItems = if (type == 2) {
        val merged = ArrayList<JSONObject>()
        data.optJSONObject("latest")?.optJSONArray("items").objects().let(merged::addAll)
        data.optJSONObject("total")?.optJSONArray("items").objects().let(merged::addAll)
        merged.distinctBy { it.s("id") }
    } else {
        data.optJSONArray("items").objects()
    }
    val parsed = rawItems.mapNotNull { mapMessageFeedItem(MessageFeedItemDto(type, it)) }
    // 补充封面 / UP 主关系：同一个视频只查一次，失败不影响文字消息。
    val enriched = enrichMessageVideos(parsed, type)
    log(
        "MSG",
        "type=$type items=${rawItems.size} parsed=${enriched.size}",
    )
    val next = (if (type == 2) data.optJSONObject("total") else data)?.optJSONObject("cursor")
    val nextId = next?.l("id") ?: 0L
    val nextTime = next?.l("time") ?: 0L
    MessageFeedPage(enriched, messageContinuation(cursor, nextId, nextTime,
        next == null || next.optBoolean("is_end", false)))
}

/**
 * 消息接口只给文字和视频 id，不给封面、也不说回复者是不是 UP 主。
 * 这里按 bvid 去重补一次视频信息；每页最多补 12 个，失败就保留纯文字卡片。
 */

internal suspend fun BiliApiContext.enrichMessageVideos(
    items: List<MessageItem>,
    type: Int,
): List<MessageItem> {
    if (items.isEmpty() || type == 1) return items
    fun resolvedBvid(item: MessageItem): String =
        item.bvid.ifBlank { aidToBvid(item.aid) }
    val targets = items
        .filter { resolvedBvid(it).isNotBlank() && (type == 0 || it.videoSubject) }
        .distinctBy { resolvedBvid(it) }
        .take(12)
    if (targets.isEmpty()) return items
    val briefs = withContext(Dispatchers.IO) {
        kotlinx.coroutines.coroutineScope {
            targets.map { item ->
                async {
                    val bvid = resolvedBvid(item)
                    val cached = synchronized(messageVideoCache) { messageVideoCache[bvid] }
                    val brief = cached ?: cancellableCatching {
                        val detail = view(bvid)
                        MessageVideoBrief(detail.cover, detail.ownerMid).also {
                            synchronized(messageVideoCache) {
                                messageVideoCache[bvid] = it
                            }
                        }
                    }.getOrNull()
                    bvid to brief
                }
            }.awaitAll()
        }
    }.filter { it.second != null }.associate { it.first to it.second!! }
    if (briefs.isEmpty()) return items
    return items.map { item ->
        val bvid = resolvedBvid(item)
        val brief = briefs[bvid] ?: return@map item
        item.copy(
            bvid = item.bvid.ifBlank { bvid },
            subjectCover = if (item.videoSubject && item.subjectCover.isBlank()) {
                brief.cover
            } else {
                item.subjectCover
            },
            isUp = item.isUp ||
                (item.userMid > 0L && brief.ownerMid > 0L && item.userMid == brief.ownerMid),
        )
    }
}

/**
 * 关注关系在不同接口里字段名不一样：`follow` / `is_follow` / `follow_status` / `attribute`。
 * 少读一个就会出现「我关注的人」永远为空。
 */

/** 消息中心会话列表：一次 25 条，按 session_ts 继续往后翻。 */

internal suspend fun BiliApiContext.messageSessions(
    followingOnly: Boolean,
    sessionTs: Long = 0L,
    pageSize: Int = 25,
    onEnriched: ((List<MessageSession>) -> Unit)? = null,
): MessageSessionPage = withContext(Dispatchers.IO) {
    val request = mutableMapOf("session_type" to "4", "group_fold" to "0",
        "unfollow_fold" to "0", "sort_rule" to "2", "size" to pageSize.toString(), "mobi_app" to "web")
    if (sessionTs > 0L) request["session_ts"] = sessionTs.toString()
    val data = dataRequest("/session_svr/v1/session_svr/get_sessions", request, base = VC)
    val allSessions = data.optJSONArray("session_list").objects()
    val raw = if (followingOnly) allSessions.filter { it.optInt("is_follow") == 1 } else allSessions
    val system = raw.map { parseMessageSession(it) }
    // 首屏先返回：内存缓存里已经有的名字/头像立即带上，其余留给后台补全。
    val quick = system.map { session ->
        val cached = messageUsers.get(session.talkerId)
        if (cached != null) {
            session.copy(name = cached.first, avatar = cached.second)
        } else {
            session
        }
    }
    val needResolve = quick
        .filter { it.name.isBlank() && it.sessionType == 1 && !it.isSystem }
    val resolved = if (needResolve.isEmpty()) quick else kotlinx.coroutines.coroutineScope {
        val limit = kotlinx.coroutines.sync.Semaphore(4)
        val names = needResolve.map { session -> async {
            limit.acquire()
            try {
                userBrief(session.talkerId)?.let { brief ->
                    messageUsers.put(session.talkerId, brief.first, brief.second)
                    session.id to brief
                }
            } finally { limit.release() }
        } }.awaitAll().filterNotNull().toMap()
        quick.map { session -> names[session.id]?.let { session.copy(name = it.first, avatar = it.second) } ?: session }
    }
    val nextTs = allSessions.lastOrNull()?.l("session_ts") ?: 0L
    val more = if (data.has("has_more")) data.optInt("has_more") == 1 else allSessions.size >= pageSize
    MessageSessionPage(resolved, sessionContinuation(sessionTs, nextTs, more))
}

/** 拉取一个私信会话的消息。 */

internal suspend fun BiliApiContext.privateMessages(talkerId: Long, sessionType: Int = 1, size: Int = 30): List<ChatMessage> =
    privateMessagePage(talkerId, sessionType, size).items
