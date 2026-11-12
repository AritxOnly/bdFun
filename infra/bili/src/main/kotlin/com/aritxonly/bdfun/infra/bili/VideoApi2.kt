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

internal suspend fun BiliApiContext.coinVideo(aid: Long, count: Int, alsoLike: Boolean): Boolean =
    withContext(Dispatchers.IO) {
        postRequest(
            "/x/web-interface/coin/add",
            mapOf(
                "aid" to aid.toString(),
                "multiply" to count.coerceIn(1, 2).toString(),
                "select_like" to if (alsoLike) "1" else "0",
                "csrf" to csrf,
            ),
        )
        true
    }

internal suspend fun BiliApiContext.triple(aid: Long): Boolean = withContext(Dispatchers.IO) {
    postRequest(
        "/x/web-interface/archive/like/triple",
        mapOf("aid" to aid.toString(), "csrf" to csrf),
    )
    true
}

/** 该视频已收藏在哪些收藏夹里（用于判断取消）。接口异常时返回空集合。 */

internal suspend fun BiliApiContext.onlineTotal(aid: Long, cid: Long): Pair<Long, String> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/x/player/online/total",
                mapOf("aid" to aid.toString(), "cid" to cid.toString()),
            )
            data.optLong("count", 0L) to data.s("total")
        }.getOrDefault(0L to "")
    }

internal suspend fun BiliApiContext.subtitles(aid: Long, cid: Long): List<SubtitleTrack> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/x/player/v2",
            mapOf("aid" to aid.toString(), "cid" to cid.toString()),
        )
        val subtitle = data.optJSONObject("subtitle") ?: return@cancellableCatching emptyList()
        subtitle.optJSONArray("subtitles").objects().mapNotNull { item ->
            val url = item.s("subtitle_url")
                .ifBlank { item.s("subtitle_url_v2") }
            if (url.isBlank()) return@mapNotNull null
            SubtitleTrack(
                id = item.s("id").ifBlank { item.s("lan") },
                label = item.s("lan_doc").ifBlank { item.s("lan") },
                url = if (url.startsWith("//")) "https:$url" else url,
                isAi = item.optInt("ai_type", 0) > 0,
            )
        }
    }.getOrDefault(emptyList())
}

/** 下载并解析字幕 JSON（B 站字幕是 `body[]` 的 from/to/content）。 */

internal suspend fun BiliApiContext.subtitleCues(url: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val request = Request.Builder()
            .url(if (url.startsWith("//")) "https:$url" else url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .get()
            .build()
        val text = http.await(request).use { response ->
            if (!response.isSuccessful) return@cancellableCatching emptyList()
            response.body?.string().orEmpty()
        }
        val root = JSONObject(text)
        root.optJSONArray("body").objects().mapNotNull { cue ->
            val content = cue.s("content").trim()
            if (content.isBlank()) return@mapNotNull null
            val from = (cue.optDouble("from", 0.0) * 1000).toLong()
            val to = (cue.optDouble("to", 0.0) * 1000).toLong()
            SubtitleCue(from, to.coerceAtLeast(from + 300L), content)
        }
    }.getOrDefault(emptyList())
}

internal fun BiliApiContext.parseSpaceVideo(item: JSONObject): VideoItem? {
    val bvid = item.s("bvid")
    if (bvid.isBlank()) return null
    return VideoItem(
        bvid = bvid,
        aid = item.l("aid"),
        title = cleanHtml(item.s("title")),
        cover = image(item.s("pic"), 480),
        ownerName = item.s("author"),
        ownerFace = "",
        ownerMid = item.l("mid"),
        play = item.l("play"),
        danmaku = item.l("video_review"),
        duration = parseDurationText(item.s("length")),
        pubdate = item.l("created"),
        cid = item.l("cid"),
    )
}

internal fun BiliApiContext.parseVideoDetail(data: JSONObject): VideoDetail =
    VideoDetailMapper.map(VideoDetailMapper.read(data), ::cleanHtml)

/** 网络类型、VPN、系统代理：用来判断是不是代理/VPN 把 playurl 换成了空内容。 */

internal fun BiliApiContext.netInfo(): String {
    val cm = appContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
        as? android.net.ConnectivityManager
    val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    val transports = buildList {
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cell")
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("ethernet")
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true) add("vpn")
    }.joinToString("+").ifBlank { "none" }
    val proxy = cancellableCatching {
        java.net.ProxySelector.getDefault()
            ?.select(java.net.URI("https://api.bilibili.com"))
            ?.firstOrNull()
            ?.toString()
    }.getOrNull()
    return "net=$transports vpn=${caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)} " +
        "proxy=$proxy"
}

/** 播放地址接口返回内容的形状，写进应用日志，方便定位“有 data 但没有地址”。 */

internal fun BiliApiContext.describePlayData(data: JSONObject): String {
    val dash = data.optJSONObject("dash")
    val video = dash?.optJSONArray("video")
    val audio = dash?.optJSONArray("audio")
    val durl = data.optJSONArray("durl")
    val firstRep = video?.optJSONObject(0)
    val firstRaw = firstRep?.let { it.s("baseUrl").ifBlank { it.s("base_url") } }.orEmpty()
    val first = firstRaw.substringBefore('?')
    val repKeys = firstRep?.keys()?.asSequence()?.take(12)?.joinToString(",").orEmpty()
    val dataKeys = data.keys().asSequence().take(16).joinToString(",")
    return "dataKeys=$dataKeys dash=${dash != null} video=${video?.length() ?: 0} " +
        "audio=${audio?.length() ?: 0} durl=${durl?.length() ?: 0} " +
        "firstBaseLen=${firstRaw.length} repKeys=$repKeys first=${first.take(80)}"
}

internal fun BiliApiContext.parsePlayInfo(
    data: JSONObject,
    preferred: Int,
    detail: VideoDetail,
    durationSec: Int = detail.duration,
    codecPreference: String = "auto",
    cdnService: String = "base",
): PlayInfo {
    val cdn = cdnServiceById(cdnService)
    val dash = data.optJSONObject("dash")
    val audio = chooseAudio(dash?.optJSONArray("audio"), cdn)
    val qualities = mutableListOf<Quality>()
    dash?.optJSONArray("video")?.objects()?.groupBy { it.optInt("id", 0) }?.forEach { (id, reps) ->
        val rep = PlaybackPolicy.chooseVideo(
            reps, codecPreference, { it.s("codecs") }, { it.l("bandwidth") },
        ) ?: return@forEach
        val baseUrl = rep.s("baseUrl").ifBlank { rep.s("base_url") }
        if (baseUrl.isBlank()) return@forEach
        val backups = jsonStrings(
            rep.optJSONArray("backupUrl") ?: rep.optJSONArray("backup_url"),
        )
        qualities += Quality(
            id = id,
            label = qualityLabel(id),
            width = rep.optInt("width", 0),
            height = rep.optInt("height", 0),
            frameRate = rep.s("frameRate").ifBlank { rep.s("fps") },
            codec = rep.s("codecs"),
            videoUrl = cdn.rewrite(baseUrl, backups),
            rawVideoUrl = baseUrl,
        )
    }
    val fallback = data.optJSONArray("durl")?.optJSONObject(0)?.s("url").orEmpty()
    val accept = data.optJSONArray("accept_quality")?.let { array ->
        (0 until array.length()).map { array.optInt(it, 0) }
    }.orEmpty()
    return PlayInfo(
        qualities = qualities.sortedByDescending { it.id },
        audioUrl = audio?.first,
        rawAudioUrl = audio?.second,
        fallbackUrl = fallback.takeIf { it.isNotBlank() }?.let { cdn.rewrite(it) },
        rawFallbackUrl = fallback.takeIf { it.isNotBlank() },
        durationMs = data.l("timelength").takeIf { it > 0 } ?: (durationSec * 1000L),
        acceptQuality = accept,
    )
}

internal fun BiliApiContext.chooseAudio(array: JSONArray?, cdn: CdnService): Pair<String, String>? {
    val audios = array.objects()
    if (audios.isEmpty()) return null
    val selected = PlaybackPolicy.chooseAudio(audios, { it.optInt("id", 0) }, { it.l("bandwidth") })
    return selected?.let { audio ->
        val url = audio.s("baseUrl").ifBlank { audio.s("base_url") }
        cdn.rewrite(
            url,
            jsonStrings(audio.optJSONArray("backupUrl") ?: audio.optJSONArray("backup_url")),
        ) to url
    }
}

/** 自动选编码时的兼容性优先级，数字越小越优先。 */

internal fun BiliApiContext.codecPriority(codec: String): Int = PlaybackPolicy.codecPriority(codec)
internal fun BiliApiContext.codecMatches(codec: String, preference: String): Boolean =
    PlaybackPolicy.codecMatches(codec, preference)
