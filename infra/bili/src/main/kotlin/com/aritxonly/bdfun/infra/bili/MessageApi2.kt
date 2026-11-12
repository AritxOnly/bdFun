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
    mapPrivateMessagePage(data, mid, before)
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
    mapNewFollowers(data)
}

/** 仅取用户昵称和头像，供私信会话补全。 */

internal suspend fun BiliApiContext.messageFeedUnread(): Map<String, Int> {
    val data = dataRequest("/x/msgfeed/unread", mapOf("mobi_app" to "web", "build" to "0"))
    return mapFeedUnreadCounts(data)
}

internal suspend fun BiliApiContext.messageUnreadCount(): Int = withContext(Dispatchers.IO) {
    val params = mapOf("build" to "0", "mobi_app" to "web", "web_location" to "333.1365")
    val privateData = dataRequest("/session_svr/v1/session_svr/single_unread",
        params + ("unread_type" to "0"), base = "https://api.vc.bilibili.com")
    val feed = dataRequest("/x/msgfeed/unread", params)
    mapTotalUnread(privateData, feed)
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
    val items = mapSystemMessages(root)
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
