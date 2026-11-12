package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.comment.CommentExtras
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

internal fun BiliApiContext.parseComment(
    reply: JSONObject,
    upMid: Long,
    top: Boolean,
    depth: Int = 0,
): CommentItem {
    val member = reply.optJSONObject("member")
    val content = reply.optJSONObject("content")
    val mid = member?.l("mid") ?: 0
    val emotes = mutableMapOf<String, String>()
    content?.optJSONObject("emote")?.let { obj ->
        obj.keys().forEach { rawKey ->
            // 接口里的 key 是 "[doge]"，正文里是 [doge]，统一成不带括号的 "doge"
            val key = rawKey.removeSurrounding("[", "]").trim()
            val url = obj.optJSONObject(rawKey)?.s("url").orEmpty()
            if (key.isNotBlank() && url.isNotBlank()) emotes[key] = url
        }
    }
    val pictures = content?.optJSONArray("pictures")?.objects()?.mapNotNull { pic ->
        val url = pic.s("img_src").ifBlank { pic.s("url") }
        if (url.isBlank()) null else image(url, 720)
    }.orEmpty()
    // 正文里 @ 的人；官方接口会在 content.members 里给出 mid。
    val mentions = content?.optJSONArray("members")?.objects()?.mapNotNull { m ->
        val name = m.s("uname").ifBlank { m.s("name") }
        val memberMid = m.l("mid")
        if (name.isBlank() || memberMid <= 0L) null else CommentMention(memberMid, name)
    }.orEmpty()
    return CommentItem(
        rpid = reply.l("rpid"),
        uname = member?.s("uname").orEmpty(),
        avatar = avatar(member?.s("avatar").orEmpty(), 120),
        message = content?.s("message").orEmpty(),
        like = reply.optInt("like", 0),
        ctime = reply.l("ctime"),
        level = member?.optJSONObject("level_info")?.optInt("current_level") ?: 0,
        replyCount = reply.optInt("rcount", 0),
        isUp = upMid > 0 && mid == upMid,
        isTop = top,
        liked = reply.optInt("action", 0) == 1,
        extras = CommentExtras(
            mid = mid,
            oid = reply.l("oid"),
            root = reply.l("root"),
            parent = reply.l("parent"),
            emotes = emotes,
            ipLocation = reply.optJSONObject("reply_control")?.s("location").orEmpty(),
            isVip = member?.optJSONObject("vip")?.optInt("vipStatus", 0) == 1,
            pictures = pictures,
            mentions = mentions,
            previewReplies = if (depth < 1) {
            reply.optJSONArray("replies")?.objects()
                ?.take(3)
                ?.map { parseComment(it, upMid, false, depth + 1) }
                .orEmpty()
        } else {
            emptyList()
        },
        ),
    )
}

// ---------------------------------------------------------------- request
