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
    val parsed = rawItems.mapNotNull { item ->
        val detail = item.optJSONObject("item")
        // 收到的赞把用户放在 users 数组里，其余类型是单个 user 对象
        val users = item.optJSONArray("users").objects()
        val user = item.optJSONObject("user") ?: users.firstOrNull()
        val names = users.mapNotNull {
            it.s("nickname").ifBlank { it.s("uname") }.takeIf(String::isNotBlank)
        }
        val detailType = detail?.s("type").orEmpty()
        val isLike = type == 2
        val isAt = type == 1 || detailType == "at"
        val sourceContent = detail?.s("source_content").orEmpty()
        val targetContent = detail?.s("target_reply_content").orEmpty()
        val rootContent = detail?.s("root_reply_content").orEmpty()
        val businessId = detail?.optInt("business_id", 0) ?: 0
        val rawContent = item.s("content")
            .ifBlank { detail?.s("content").orEmpty() }
            .ifBlank { detail?.s("message").orEmpty() }
        // 回复通知的正文经常只放在 source_content 里；点赞通知优先保留原始正文，
        // 这样「收到喜欢」右侧才能显示被点赞的评论原文。
        val content = if (isLike) rawContent else rawContent.ifBlank { sourceContent }
        val likeVideo = isLike && (
            detailType == "video" || detailType == "archive" ||
                (businessId == 1 && sourceContent.isBlank() &&
                    targetContent.isBlank() && rootContent.isBlank() && rawContent.isBlank())
            )
        val commentOnMyVideo = !isLike && !isAt &&
            (detailType == "video" ||
                businessId == 1 && targetContent.isBlank() && rootContent.isBlank())
        val action = when {
            isLike && likeVideo -> "赞了我的视频"
            isLike -> "赞了我的评论"
            isAt -> "提到了我"
            commentOnMyVideo -> "对我的视频发表了评论"
            else -> "回复了我的评论"
        }
        // 右侧：层主评论 > 我原来的评论；评论我视频时固定用视频封面。
        val subjectText = when {
            commentOnMyVideo -> ""
            isLike -> if (likeVideo) "" else sourceContent
                .ifBlank { targetContent }
                .ifBlank { rootContent }
                .ifBlank { rawContent }
            rootContent.isNotBlank() -> rootContent
            targetContent.isNotBlank() -> targetContent
            else -> ""
        }
        // 底部引用只在线程里出现：我在这层楼里说过的话。
        val quote = if (!isLike && rootContent.isNotBlank()) targetContent else ""
        val source = sourceContent
            .ifBlank { rootContent }
            .ifBlank { targetContent }
            .ifBlank { rawContent }
            .ifBlank { detail?.s("desc").orEmpty() }
            .ifBlank { item.s("title") }
        val title = detail?.s("title").orEmpty().ifBlank { item.s("title") }
        val subjectCover = if (likeVideo || commentOnMyVideo) {
            detail?.s("cover").orEmpty()
                .ifBlank { detail?.s("pic").orEmpty() }
        } else {
            ""
        }
        val videoSubject = likeVideo || commentOnMyVideo
        val subjectId = (detail?.l("subject_id") ?: 0L)
            .takeIf { it > 0L }
            ?: item.l("subject_id")
        val nativeUri = detail?.s("native_uri").orEmpty()
            .ifBlank { detail?.s("uri").orEmpty() }
            .ifBlank { item.s("native_uri").orEmpty() }
        // 消息接口最可靠的是 native_uri：bilibili://video/{aid}/?comment_root_id=..&comment_secondary_id=..
        // 之前只看 bvid / subject_id，回复消息里这两项经常为空或是别的东西，导致跳过去是空的。
        val uriAid = listOf(
            "bilibili://video/(\\d+)",
            "bilibili://video/[aA][vV](\\d+)",
            "bilibili\\.com/video/[aA][vV](\\d+)",
        ).firstNotNullOfOrNull { pattern ->
            Regex(pattern).find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull()
        }
        val uriBvid = Regex("BV[0-9A-Za-z]{10}").find(nativeUri)?.value.orEmpty()
        val uriRootRpid = Regex("[?&]comment_root_id=(\\d+)")
            .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val uriSecondRpid = Regex("[?&]comment_secondary_id=(\\d+)")
            .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val bvid = uriBvid.ifBlank {
            detail?.s("bvid").orEmpty()
        }
        val aid = when {
            uriAid != null && uriAid > 0L -> uriAid
            subjectId > 0L -> subjectId
            else -> (detail?.l("oid") ?: 0L).takeIf { it > 0L }
                ?: (item.l("oid") ?: 0L)
        }
        val seekSeconds = Regex("[?&#](?:t|start|time)=([0-9]+)")
            .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        // 点赞通知的文字字段经常是空的，只要有用户就保留，否则整页会「没有内容」。
        if (type != 2 && title.isBlank() && content.isBlank() && source.isBlank()) {
            return@mapNotNull null
        }
        MessageItem(
            id = item.s("id").ifBlank { "${type}_${item.l("id")}" },
            title = title,
            content = content,
            time = item.l("reply_time")
                .takeIf { it > 0 }
                ?: item.l("ctime").takeIf { it > 0 }
                ?: item.l("timestamp").takeIf { it > 0 }
                ?: item.l("like_time").takeIf { it > 0 }
                ?: (item.s("pub_time").toLongOrNull() ?: 0L),
            name = user?.s("nickname").orEmpty().ifBlank { user?.s("uname").orEmpty() },
            avatar = avatar(user?.s("avatar").orEmpty(), 96),
            bvid = bvid,
            action = action,
            source = source,
            userMid = user?.l("mid") ?: 0L,
            followed = isFollowedUser(user, item),
            likeNames = names,
            likeCount = maxOf(
                item.optInt("counts", 0),
                detail?.optInt("counts", 0) ?: 0,
                names.size,
            ),
            aid = aid,
            rootRpid = uriRootRpid.takeIf { it > 0L } ?: (detail?.l("root_id") ?: 0L),
            targetRpid = uriSecondRpid.takeIf { it > 0L } ?: (detail?.l("target_id") ?: 0L),
            sourceRpid = uriSecondRpid.takeIf { it > 0L }
                ?: (detail?.l("source_id") ?: 0L),
            businessId = businessId,
            seekMs = seekSeconds * 1000L,
            nativeUri = nativeUri,
            noticeState = item.optInt("notice_state", 0),
            quote = quote,
            subjectText = subjectText,
            subjectCover = subjectCover,
            videoSubject = videoSubject,
            atMe = isAt,
            commentOnMyVideo = commentOnMyVideo,
        )
    }
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

internal fun BiliApiContext.isFollowedUser(user: JSONObject?, item: JSONObject): Boolean {
    fun followed(obj: JSONObject?): Boolean {
        if (obj == null) return false
        if (obj.optBoolean("follow", false)) return true
        if (obj.optBoolean("is_follow", false)) return true
        if (obj.optInt("is_follow", 0) > 0) return true
        if (obj.optInt("follow_status", 0) > 0) return true
        val attribute = obj.optInt("attribute", 0)
        // 1=悄悄关注 / 2=已关注 / 6=互关，其余（拉黑等）不算。
        return attribute == 1 || attribute == 2 || attribute == 6
    }
    return followed(user) || followed(item)
}

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
