package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.message.MessagePresentation

import com.aritxonly.bdfun.core.model.message.MessageNotice

import com.aritxonly.bdfun.core.model.message.MessageActor

import com.aritxonly.bdfun.core.model.message.MessageSessionPreferences

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

internal suspend fun BiliApiContext.privateMessagePage(
    talkerId: Long,
    sessionType: Int = 1,
    size: Int = 30,
    before: Long? = null,
): ChatMessagePage = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/svr_sync/v1/svr_sync/fetch_session_msgs",
        mapOf(
            "talker_id" to talkerId.toString(),
            "session_type" to sessionType.toString(),
            "size" to size.toString(),
            "sender_device_id" to "1",
            "build" to "0",
            "mobi_app" to "web",
            "web_location" to "333.1296",
        ) + (before?.let { mapOf("begin_seqno" to it.toString()) } ?: emptyMap()),
        signed = true,
        base = VC,
    )
    val own = mid
    val items = data.optJSONArray("messages").objects().mapNotNull { message ->
        val type = message.optInt("msg_type", 1)
        val raw = message.s("content")
        val content = cancellableCatching { JSONObject(raw) }.getOrNull()
        val withdrawn = message.optInt("msg_status") == 1
        val text = if (withdrawn) "消息已撤回" else when (type) {
            1 -> content?.s("content").orEmpty()
            2 -> ""
            6, 7 -> content?.s("title").orEmpty()
            else -> content?.s("content").orEmpty().ifBlank { raw }
        }
        val image = if (type == 2 && !withdrawn) content?.s("url").orEmpty() else ""
        if (text.isBlank() && image.isBlank() && type != 1) return@mapNotNull null
        ChatMessage(
            id = message.l("msg_key").takeIf { it != 0L }
                ?: message.l("msg_seqno"),
            sequence = message.l("msg_seqno"),
            senderId = message.l("sender_uid"),
            timestamp = message.l("timestamp").takeIf { it > 0L }
                ?: message.l("msg_time"),
            text = text,
            sharedVideo = decodeSharedVideo(text),
            imageUrl = image,
            isMine = message.l("sender_uid") == own,
        )
    }.sortedBy { it.sequence }
    val next = data.optString("min_seqno").toLongOrNull() ?: 0L
    ChatMessagePage(items, sessionContinuation(before ?: 0L, next, data.optInt("has_more") == 1))
}

/** 发送一条纯文本私信。 */

internal suspend fun BiliApiContext.sendPrivateMessage(talkerId: Long, text: String, sessionType: Int = 1,
    imageContent: String? = null): Boolean =
    withContext(Dispatchers.IO) {
        if ((text.isBlank() && imageContent == null) || csrf.isBlank()) return@withContext false
        val now = System.currentTimeMillis() / 1000L
        val content = imageContent ?: JSONObject().put("content", text).toString()
        val params = mapOf(
            "msg[sender_uid]" to mid.toString(),
            "msg[receiver_id]" to talkerId.toString(),
            "msg[receiver_type]" to sessionType.toString(),
            "msg[msg_type]" to if (imageContent != null) "2" else "1",
            "msg[msg_status]" to "0",
            "msg[dev_id]" to java.util.UUID.randomUUID().toString(),
            "msg[timestamp]" to now.toString(),
            "msg[new_face_version]" to "1",
            "msg[content]" to content,
            "from_firework" to "0",
            "build" to "0",
            "mobi_app" to "web",
            "csrf" to csrf,
            "csrf_token" to csrf,
        )
        val root = cancellableCatching {
            rawRequest(
                "/web_im/v1/web_im/send_msg",
                params,
                base = VC,
                method = "POST",
                tolerateEmptyBody = false,
            )
        }.getOrNull() ?: return@withContext false
        root.optInt("code", 0) == 0
    }

/** 官方「新增粉丝」：/x/relation/fans。 */

internal suspend fun BiliApiContext.newFollowers(page: Int = 1): List<MessageItem> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/relation/fans",
        mapOf(
            "vmid" to mid.toString(),
            "pn" to page.toString(),
            "ps" to "20",
            "order" to "desc",
            "order_type" to "attention",
        ),
    )
    data.optJSONArray("list").objects().mapNotNull { fan ->
        val fanMid = fan.l("mid")
        if (fanMid <= 0L) return@mapNotNull null
        val attribute = fan.optInt("attribute", 0)
        MessageItem(
            id = "fans_$fanMid",
            title = "",
            content = fan.s("sign"),
            time = fan.l("mtime").takeIf { it > 0L } ?: fan.l("ctime"),
            name = fan.s("uname"),
            avatar = avatar(fan.s("face"), 120),
            presentation = MessagePresentation(
                action = "关注了我",
                source = fan.s("sign"),
            ),
            actor = MessageActor(
                userMid = fanMid,
                followed = attribute == 2 || attribute == 6 ||
                fan.optBoolean("is_follow", false),
            ),
        )
    }
}

/** 仅取用户昵称和头像，供私信会话补全。 */

internal suspend fun BiliApiContext.messageFeedUnread(): Map<String, Int> {
    val data = dataRequest("/x/msgfeed/unread", mapOf("mobi_app" to "web", "build" to "0"))
    return listOf("reply", "at", "like", "sys_msg").associateWith { data.optInt(it).coerceAtLeast(0) }
}

internal suspend fun BiliApiContext.messageUnreadCount(): Int = withContext(Dispatchers.IO) {
    val params = mapOf("build" to "0", "mobi_app" to "web", "web_location" to "333.1365")
    val privateData = dataRequest("/session_svr/v1/session_svr/single_unread",
        params + ("unread_type" to "0"), base = "https://api.vc.bilibili.com")
    val feed = dataRequest("/x/msgfeed/unread", params)
    (listOf("follow_unread", "unfollow_unread").sumOf { privateData.optInt(it).coerceAtLeast(0) } +
        listOf("at", "like", "reply", "sys_msg").sumOf { feed.optInt(it).coerceAtLeast(0) })
}

/** Only report success after the server confirms that every unread category is clear. */

internal suspend fun BiliApiContext.clearAllUnread(sessions: List<MessageSession>): Boolean = withContext(Dispatchers.IO) {
    val all = sessions.associateBy { it.id }.toMutableMap()
    var cursor = 0L
    do {
        val page = messageSessions(false, cursor)
        page.items.forEach { all[it.id] = it }
        cursor = page.next ?: 0L
    } while (cursor != 0L)
    markSessionsRead(all.values.toList())
    // These feeds acknowledge their current notifications when opened, as in HyperPiliPlus.
    for (type in 0..3) messagesPage(type)
    messageUnreadCount() == 0
}

/** 仅取用户 IP 属地，供动态详情在接口未返回发布 IP 时兜底。 */

internal suspend fun BiliApiContext.markSessionsRead(sessions: List<MessageSession>): Boolean = withContext(Dispatchers.IO) {
    acknowledgeUnreadSessions(sessions) { session ->
        dataRequest("/session_svr/v1/session_svr/update_ack", sessionParams(session) +
            ("ack_seqno" to session.maxSeqno.toString()), base = VC, signed = true)
    }
    true
}

internal fun BiliApiContext.sessionParams(session: MessageSession) = mapOf(
    "talker_id" to session.talkerId.toString(), "session_type" to session.sessionType.toString(),
    "csrf" to csrf, "csrf_token" to csrf, "mobi_app" to "web", "build" to "0")

internal suspend fun BiliApiContext.removeMessageSession(session: MessageSession) {
    dataRequest("/session_svr/v1/session_svr/remove_session", sessionParams(session),
        base = VC, signed = true, method = "POST")
}

internal suspend fun BiliApiContext.pinMessageSession(session: MessageSession, pinned: Boolean) {
    dataRequest("/session_svr/v1/session_svr/set_top", sessionParams(session) +
        ("op_type" to sessionPinOperation(pinned)), base = VC, signed = true, method = "POST")
}

internal suspend fun BiliApiContext.muteMessageSession(session: MessageSession, muted: Boolean) {
    dataRequest("/link_setting/v1/link_setting/set_msg_dnd", mapOf(
        "uid" to mid.toString(), "dnd_uid" to session.talkerId.toString(),
        "setting" to if (muted) "1" else "0", "csrf" to csrf, "mobi_app" to "web", "build" to "0"),
        base = VC, method = "POST")
}

internal suspend fun BiliApiContext.deleteFeedMessage(type: Int, id: String) {
    require(type in 0..2)
    postRequest("/x/msgfeed/del", mapOf("tp" to messageDeleteType(type).toString(), "id" to id,
        "csrf" to csrf, "csrf_token" to csrf, "mobi_app" to "web", "build" to "0"), tolerateEmptyBody = false)
}

internal suspend fun BiliApiContext.setLikeNotice(id: String, enabled: Boolean) {
    postRequest("/x/msgfeed/notice", mapOf("tp" to "0", "id" to id,
        "notice_state" to if (enabled) "0" else "1", "platform" to "web", "mobi_app" to "web",
        "build" to "0", "csrf" to csrf), tolerateEmptyBody = false)
}

internal suspend fun BiliApiContext.systemMessages(cursor: MessageCursor?): MessageFeedPage {
    val params = mutableMapOf("page_size" to "20", "mobi_app" to "web", "build" to "0")
    cursor?.let { params["cursor"] = it.id.toString() }
    val root = rawRequest("/x/sys-msg/query_notify_list", params, base = "https://message.bilibili.com")
    val code = root.optInt("code")
    if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
    val items = root.optJSONArray("data").objects().map {
        val raw = it.s("content")
        val content = cancellableCatching { JSONObject(raw).optString("web", raw) }.getOrDefault(raw)
        MessageItem(
            id = it.s("id"),
            title = it.s("title"),
            content = content,
            time = 0L,
            name = "系统通知",
            avatar = "",
            presentation = MessagePresentation(
                source = it.s("time_at"),
            ),
            notice = MessageNotice(
                systemCursor = it.l("cursor"),
            ),
        )
    }
    if (cursor == null && items.isNotEmpty()) {
        try {
            dataRequest("/x/sys-msg/update_cursor", mapOf("cursor" to items.first().systemCursor.toString(),
                "csrf" to csrf), base = "https://message.bilibili.com")
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { log("MSG", "system read acknowledgement failed") }
    }
    val next = items.lastOrNull()?.systemCursor ?: 0L
    return MessageFeedPage(items, messageContinuation(cursor, next, 0L, items.size < 20))
}

internal suspend fun BiliApiContext.deleteSystemMessage(id: String) = withContext(Dispatchers.IO) {
    val body = JSONObject().put("csrf", csrf).put("ids", JSONArray().put(id.toLong()))
        .put("station_ids", JSONArray()).put("type", 4).put("mobi_app", "android")
    val request = Request.Builder()
        .url("https://message.bilibili.com/x/sys-msg/del_notify_list?mobi_app=android&csrf=${encode(csrf)}")
        .header("User-Agent", UA).header("Referer", "https://message.bilibili.com/")
        .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull())).build()
    val response = http.executeOnce(request, false)
    val code = response.optInt("code")
    if (code != 0) throw BiliException(code, apiMessage(code, response.optString("message")))
}

internal fun BiliApiContext.parseMessageSession(message: JSONObject): MessageSession = with(message) {
    val type = optInt("session_type", 1)
    val systemType = optInt("system_msg_type", 0)
    val account = optJSONObject("account_info")
    val name = account?.s("name").orEmpty()
        .ifBlank { if (type == 2) s("group_name") else "" }
    val avatar = account?.s("pic_url").orEmpty()
        .ifBlank { if (type == 2) s("group_cover") else "" }
    val last = optJSONObject("last_msg")
    MessageSession(
        id = "${type}_${l("talker_id")}",
        talkerId = l("talker_id"),
        sessionType = type,
        name = name,
        avatar = avatar,
        preview = last?.let { previewOfMessage(it) }.orEmpty(),
        time = (l("session_ts") / 1_000_000L).takeIf { it > 0L }
            ?: (last?.l("timestamp") ?: 0L),
        unread = optInt("unread_count", 0),
        isSystem = systemType != 0,
        preferences = MessageSessionPreferences(
            followed = optInt("is_follow") == 1,
            pinned = l("top_ts") > 0L,
            muted = optInt("is_dnd") == 1,
        ),
        maxSeqno = l("max_seqno"),
        sessionTs = l("session_ts"),
    )
}

internal fun BiliApiContext.previewOfMessage(message: JSONObject): String {
    val type = message.optInt("msg_type", 0)
    val raw = message.optString("content")
    val obj = cancellableCatching { JSONObject(raw) }.getOrNull()
    return when (type) {
        2 -> "[图片]"
        306 -> obj?.s("content").orEmpty()
        10, 11, 12 -> obj?.s("title").orEmpty()
            .ifBlank { obj?.s("text").orEmpty() }
            .ifBlank { obj?.s("desc").orEmpty() }
        else -> obj?.s("content").orEmpty()
            .ifBlank { obj?.s("title").orEmpty() }
            .ifBlank { obj?.s("text").orEmpty() }
            .ifBlank { raw }
    }.replace('\n', ' ')
}

// ---------------------------------------------------------------- parsing
