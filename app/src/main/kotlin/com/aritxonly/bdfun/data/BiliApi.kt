package com.aritxonly.bdfun.data

import android.content.Context
import android.net.Uri
import android.text.Html
import com.aritxonly.bdfun.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

class BiliApi(context: Context) {
    private val appContext: Context = context.applicationContext
    val cookieJar = BiliCookieJar(context)

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        // execute() 会绕过 OkHttp 的并发队列；改成 enqueue/await 后由 Dispatcher 统一排队和限流。
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 32
                maxRequestsPerHost = 8
            },
        )
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val buvidMutex = Mutex()
    private val wbiMutex = Mutex()
    /** 历史记录补播放量/弹幕数的缓存：bvid -> (play, danmaku) */
    private val videoStatCache =
        java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Long>>()

    @Volatile
    private var wbiKeys: WbiKeys? = null

    /** buvid 获取失败后的冷却时间，避免每个请求都重新卡在一次失败请求上。 */
    @Volatile
    private var buvidRetryAt = 0L

    /** WBI key 的获取时间；key 会轮换，进程活太久时要重新拉。 */
    @Volatile
    private var wbiFetchedAt = 0L

    val isLoggedIn: Boolean get() = cookieJar.hasSession
    val mid: Long get() = cookieJar.mid
    val csrf: String get() = cookieJar.csrf

    // ---------------------------------------------------------------- auth

    suspend fun nav(): UserProfile? = withContext(Dispatchers.IO) {
        val root = rawRequest("/x/web-interface/nav", emptyMap())
        parseWbi(root.optJSONObject("data")?.optJSONObject("wbi_img"))
        val data = root.optJSONObject("data") ?: return@withContext null
        if (!data.optBoolean("isLogin")) return@withContext null
        val level = data.optJSONObject("level_info")?.optInt("current_level") ?: 0
        val vip = data.optJSONObject("vip_label")
        UserProfile(
            mid = data.l("mid"),
            name = data.s("uname"),
            face = avatar(data.s("face"), 160),
            sign = data.s("sign"),
            level = level,
            coins = data.optDouble("money", 0.0),
            vip = data.optInt("vipStatus") == 1,
            vipLabel = vip?.s("text").orEmpty(),
        )
    }

    suspend fun qrGenerate(): QrLogin = withContext(Dispatchers.IO) {
        val data = dataRequest(
            path = "/x/passport-login/web/qrcode/generate",
            params = mapOf("source" to "main-fe-header"),
            base = PASSPORT,
        )
        QrLogin(url = data.s("url"), key = data.s("qrcode_key"))
    }

    suspend fun qrPoll(key: String): QrPoll = withContext(Dispatchers.IO) {
        val data = dataRequest(
            path = "/x/passport-login/web/qrcode/poll",
            params = mapOf("qrcode_key" to key, "source" to "main-fe-header"),
            base = PASSPORT,
        )
        when (data.optInt("code", -1)) {
            0 -> {
                val url = data.s("url")
                saveLoginCookies(url)
                val profile = nav()
                QrPoll(QrState.Success, "登录成功", profile)
            }
            86101 -> QrPoll(QrState.Waiting, "等待扫码")
            86090 -> QrPoll(QrState.Scanned, "已扫码，请在手机上确认")
            86038 -> QrPoll(QrState.Expired, "二维码已失效")
            else -> QrPoll(QrState.Error, data.s("message").ifBlank { "登录失败" })
        }
    }

    fun logout() {
        cookieJar.clear()
        wbiKeys = null
        wbiFetchedAt = 0L
        buvidRetryAt = 0L
    }

    private fun saveLoginCookies(url: String) {
        val uri = Uri.parse(url)
        listOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5").forEach { name ->
            uri.getQueryParameter(name)?.takeIf { it.isNotBlank() }?.let { cookieJar.put(name, it) }
        }
    }

    // ---------------------------------------------------------------- feed

    suspend fun recommend(freshIndex: Int = 1, web: Boolean = true): List<VideoItem> = withContext(Dispatchers.IO) {
        var primaryError: Exception? = null
        val primary = try {
            if (web) webRecommend(freshIndex) else appRecommend(freshIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            primaryError = e
            emptyList()
        }
        if (primary.isNotEmpty()) return@withContext primary

        // 纯网络错误说明整个网络都不通，立刻报错，避免再用另一个接口白等一轮。
        val netError = primaryError
        if (netError is BiliException && netError.code == -1) throw netError

        // Web 推荐失败/为空时用 App 推荐兜底，反之亦然；总比整页空白强。
        val fallback = try {
            if (web) appRecommend(freshIndex) else webRecommend(freshIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        if (fallback.isNotEmpty()) return@withContext fallback
        if (primaryError != null) throw primaryError
        emptyList()
    }

    private suspend fun webRecommend(freshIndex: Int): List<VideoItem> {
        val data = dataRequest(
            "/x/web-interface/wbi/index/top/feed/rcmd",
            mapOf(
                "web_location" to "1430650",
                "y_num" to "5",
                "fresh_type" to "4",
                "feed_version" to "V8",
                "fresh_idx_1h" to freshIndex.toString(),
                "fetch_row" to "1",
                "fresh_idx" to freshIndex.toString(),
                "brush" to "0",
                "homepage_ver" to "1",
                "ps" to "12",
            ),
            signed = true,
        )
        return data.optJSONArray("item").objects().mapNotNull(::parseFeedVideo)
    }

    private suspend fun appRecommend(freshIndex: Int): List<VideoItem> {
        val data = dataRequest(
            "/x/v2/feed/index",
            mapOf(
                "build" to "7880300",
                "mobi_app" to "android",
                "platform" to "android",
                "idx" to freshIndex.toString(),
                "login_event" to "0",
                "network" to "wifi",
                "style" to "2",
                "ps" to "12",
            ),
        )
        return data.optJSONArray("items").objects().mapNotNull { raw ->
            val args = raw.optJSONObject("args")
                ?: runCatching { JSONObject(raw.optString("args")) }.getOrNull()
                ?: return@mapNotNull null
            val bvid = args.s("bvid")
            if (bvid.isBlank()) return@mapNotNull null
            VideoItem(
                bvid = bvid,
                aid = args.l("aid"),
                title = cleanHtml(args.s("title")),
                cover = image(args.s("cover"), 480),
                ownerName = args.s("up_name").ifBlank { args.s("name") },
                ownerFace = avatar(args.s("up_face").ifBlank { args.s("face") }, 96),
                ownerMid = args.l("up_id").takeIf { it > 0 } ?: args.l("mid"),
                play = args.l("play"),
                danmaku = args.l("danmaku"),
                duration = args.optInt("duration", 0),
                pubdate = args.l("ctime"),
                cid = args.l("cid"),
            )
        }
    }

    suspend fun popular(page: Int = 1): List<VideoItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/popular",
            mapOf("pn" to page.toString(), "ps" to "20"),
        )
        data.optJSONArray("list").objects().mapNotNull(::parseFeedVideo)
    }

    suspend fun ranking(rid: Int = 0): List<VideoItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/ranking/v2",
            mapOf("rid" to rid.toString(), "type" to "all"),
        )
        data.optJSONArray("list").objects().mapNotNull(::parseFeedVideo)
    }

    // ---------------------------------------------------------------- video

    suspend fun view(bvid: String): VideoDetail = withContext(Dispatchers.IO) {
        val data = dataRequest("/x/web-interface/view", mapOf("bvid" to bvid))
        parseVideoDetail(data)
    }

    suspend fun playInfo(
        detail: VideoDetail,
        preferredQuality: Int,
        cid: Long = detail.cid,
        durationSec: Int = detail.duration,
        codecPreference: String = "auto",
        cdnService: String = "base",
    ): PlayInfo = withContext(Dispatchers.IO) {
        val params = mapOf(
            "bvid" to detail.bvid,
            "cid" to cid.toString(),
            "qn" to "127",
            "fnval" to "4048",
            "fnver" to "0",
            "fourk" to "1",
        )
        // 高清（1080P+/4K/HDR/杜比）现在需要 WBI 签名；先走签名接口，
        // 失败再退回普通接口。反过来的话，普通接口有时只回 480P 但不报错，
        // 4K 就会被悄悄吞掉。
        var data = runCatching {
            dataRequest("/x/player/wbi/playurl", params, signed = true)
        }.getOrNull()
        if (data == null) {
            data = dataRequest("/x/player/playurl", params)
        }
        val parsed = parsePlayInfo(
            data,
            preferredQuality,
            detail,
            durationSec,
            codecPreference,
            cdnService,
        )
        if (parsed.fallbackUrl == null && parsed.qualities.isNotEmpty()) {
            // DASH 解码失败时的兜底：优先 1080P，再 720P，最后才 480P。
            // 以前固定 480P，4K 解码一出错就整段变糊，看起来像“4K 没用”。
            var fallbackUrl = ""
            for (qn in listOf("80", "64", "32")) {
                val fallbackData = runCatching {
                    dataRequest(
                        "/x/player/playurl",
                        mapOf(
                            "bvid" to detail.bvid,
                            "cid" to cid.toString(),
                            "qn" to qn,
                            "fnval" to "1",
                            "fnver" to "0",
                        ),
                    )
                }.getOrNull() ?: continue
                val candidate = fallbackData.optJSONArray("durl")
                    ?.optJSONObject(0)
                    ?.s("url")
                    .orEmpty()
                if (candidate.isNotBlank()) {
                    fallbackUrl = candidate
                    break
                }
            }
            if (fallbackUrl.isNotBlank()) parsed.copy(fallbackUrl = fallbackUrl) else parsed
        } else {
            parsed
        }
    }

    suspend fun related(bvid: String): List<VideoItem> = withContext(Dispatchers.IO) {
        // 该接口的 data 是数组而不是对象，不能用 dataRequest
        val root = rawRequest("/x/web-interface/archive/related", mapOf("bvid" to bvid))
        if (root.optInt("code", 0) != 0) return@withContext emptyList()
        root.optJSONArray("data").objects().mapNotNull(::parseFeedVideo)
    }

    suspend fun danmaku(cid: Long): List<DanmakuItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$API/x/v1/dm/list.so?oid=$cid")
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .get()
            .build()
        val bytes = client.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            response.body?.bytes() ?: return@withContext emptyList()
        }
        val xml = decodeDanmakuBytes(bytes)
        parseDanmakuXml(xml)
    }

    suspend fun comments(
        aid: Long,
        page: Int,
        upMid: Long = 0,
        sort: Int = 2,
    ): CommentPage = commentsByOid(aid.toString(), 1, page, upMid, sort)

    /**
     * 通用评论区：视频用 type=1 + aid，动态用 type=17 + id_str。
     * 两条路只是 oid/type 不同，解析和分页完全一致。
     */
    suspend fun commentsByOid(
        oid: String,
        type: Int,
        page: Int,
        upMid: Long = 0,
        sort: Int = 2,
    ): CommentPage = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/v2/reply",
            mapOf(
                "type" to type.toString(),
                "oid" to oid,
                "pn" to page.toString(),
                "ps" to "20",
                "sort" to sort.toString(),
            ),
        )
        val list = mutableListOf<CommentItem>()
        data.optJSONArray("top")?.objects()?.forEach { list += parseComment(it, upMid, true) }
        data.optJSONArray("replies")?.objects()?.forEach { list += parseComment(it, upMid, false) }
        val pageData = data.optJSONObject("page")
        val count = pageData?.optInt("count") ?: 0
        CommentPage(
            items = list.distinctBy { it.rpid },
            page = page,
            isEnd = page >= count,
            total = pageData?.optInt("acount") ?: 0,
        )
    }

    suspend fun replies(aid: Long, root: Long, page: Int = 1, upMid: Long = 0): CommentPage =
        repliesByOid(aid.toString(), 1, root, page, upMid)

    suspend fun repliesByOid(
        oid: String,
        type: Int,
        root: Long,
        page: Int = 1,
        upMid: Long = 0,
    ): CommentPage = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/v2/reply/reply",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "root" to root.toString(),
                "pn" to page.toString(),
                "ps" to "20",
            ),
        )
        val list = data.optJSONArray("replies").objects().map { parseComment(it, upMid, false) }
        val pageData = data.optJSONObject("page")
        val count = pageData?.optInt("count") ?: 0
        CommentPage(
            items = list.distinctBy { it.rpid },
            page = page,
            isEnd = page >= count,
            total = pageData?.optInt("acount") ?: 0,
        )
    }

    suspend fun addComment(
        aid: Long,
        message: String,
        root: Long = 0,
        parent: Long = 0,
        pictures: String? = null,
    ): Long? = addCommentByOid(aid.toString(), 1, message, root, parent, pictures)

    suspend fun addCommentByOid(
        oid: String,
        type: Int,
        message: String,
        root: Long = 0,
        parent: Long = 0,
        pictures: String? = null,
    ): Long? = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "oid" to oid,
            "type" to type.toString(),
            "message" to message,
            "plat" to "1",
            "csrf" to csrf,
        )
        if (root > 0) params["root"] = root.toString()
        if (parent > 0) params["parent"] = parent.toString()
        if (!pictures.isNullOrBlank()) params["pictures"] = pictures
        val data = postRequest("/x/v2/reply/add", params, tolerateEmptyBody = false)
        data.l("rpid").takeIf { it > 0 }
            ?: data.optJSONObject("reply")?.l("rpid")?.takeIf { it > 0 }
            ?: 0L
    }

    /** 上传评论图片，返回可直接塞进 `pictures` 的 JSON；失败返回 null。 */
    suspend fun uploadCommentImage(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = appContext.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val width = bitmap?.width ?: 0
            val height = bitmap?.height ?: 0
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image",
                    bytes.toRequestBody(mime.toMediaTypeOrNull()),
                )
                .addFormDataPart("csrf", csrf)
                .build()
            val request = Request.Builder()
                .url("$API/x/dynamic/feed/dyn_img/upload")
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .header("Origin", "https://www.bilibili.com")
                .post(body)
                .build()
            val json = client.newCall(request).awaitResponse().use { response ->
                val text = response.body?.string().orEmpty()
                if (text.isBlank()) return@use null
                JSONObject(text)
            } ?: return@runCatching null
            if (json.optInt("code", 0) != 0) return@runCatching null
            val data = json.optJSONObject("data") ?: return@runCatching null
            val url = data.s("image_url").ifBlank { data.s("url") }
            if (url.isBlank()) return@runCatching null
            JSONObject().apply {
                put("img_src", url)
                put("img_width", width)
                put("img_height", height)
            }.toString()
        }.getOrNull()
    }

    /** 发送弹幕（videoup 的普通滚动弹幕）。 */
    suspend fun sendDanmaku(
        cid: Long,
        bvid: String,
        text: String,
        progressMs: Long = 0L,
        mode: Int = 1,
        fontSize: Int = 25,
        color: Int = 0xFFFFFF,
    ): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/dm/post",
            mapOf(
                "type" to "1",
                "oid" to cid.toString(),
                "bvid" to bvid,
                "msg" to text,
                "progress" to progressMs.coerceAtLeast(0L).toString(),
                "mode" to mode.toString(),
                "fontsize" to fontSize.toString(),
                "color" to color.toString(),
                "pool" to "0",
                "csrf" to csrf,
            ),
        )
        true
    }

    suspend fun likeComment(aid: Long, rpid: Long, like: Boolean): Boolean =
        likeCommentByOid(aid.toString(), 1, rpid, like)

    suspend fun likeCommentByOid(
        oid: String,
        type: Int,
        rpid: Long,
        like: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/reply/action",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "rpid" to rpid.toString(),
                "action" to if (like) "1" else "0",
                "csrf" to csrf,
            ),
        )
        true
    }

    suspend fun reportComment(aid: Long, rpid: Long, reason: Int = 1): Boolean =
        reportCommentByOid(aid.toString(), 1, rpid, reason)

    suspend fun reportCommentByOid(
        oid: String,
        type: Int,
        rpid: Long,
        reason: Int = 1,
    ): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/reply/report",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "rpid" to rpid.toString(),
                "reason" to reason.toString(),
                "csrf" to csrf,
            ),
        )
        true
    }

    /** 删除自己发出的评论。 */
    suspend fun deleteComment(aid: Long, rpid: Long): Boolean =
        deleteCommentByOid(aid.toString(), 1, rpid)

    suspend fun deleteCommentByOid(oid: String, type: Int, rpid: Long): Boolean =
        withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/reply/del",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "rpid" to rpid.toString(),
                "csrf" to csrf,
            ),
        )
        true
    }

    /** 拉黑（加入黑名单）。 */
    suspend fun blockUser(mid: Long): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/relation/modify",
            mapOf(
                "fid" to mid.toString(),
                "act" to "5",
                "re_src" to "11",
                "csrf" to csrf,
            ),
        )
        true
    }

    /** 评论表情面板（需要登录；失败时返回空列表，界面回退到系统 emoji）。 */
    suspend fun replyEmotes(): List<EmoteItem> = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/emote/user/panel/web",
                mapOf("business" to "reply"),
            )
            val out = mutableListOf<EmoteItem>()
            data.optJSONArray("packages")?.objects()?.forEach { pkg ->
                pkg.optJSONArray("emote")?.objects()?.forEach { emote ->
                    val text = emote.s("text").removeSurrounding("[", "]").trim()
                    val url = image(emote.s("url"), 120)
                    if (text.isNotBlank() && url.isNotBlank()) {
                        out += EmoteItem(text = text, url = url)
                    }
                }
            }
            out
        }.getOrDefault(emptyList())
    }

    suspend fun likeVideo(aid: Long, like: Boolean): Boolean = withContext(Dispatchers.IO) {
        setVideoLike(aid, if (like) 1 else 2)
    }

    /** 点踩 / 取消点踩：like=3 踩，4 取消踩。 */
    suspend fun dislikeVideo(aid: Long, dislike: Boolean = true): Boolean =
        withContext(Dispatchers.IO) {
            setVideoLike(aid, if (dislike) 3 else 4)
        }

    /**
     * 点赞 / 点踩统一入口。
     * 走 web 的 `archive/like`：1 点赞、2 取消赞、3 点踩、4 取消踩。
     * 旧的 `/x/v2/view/like` 已下线（404 返回 HTML，导致“解析失败”），App 端
     * `/x/v2/view/like` 只认 access_key，用网页 cookie 会返回 -101，所以这里只用 web 接口。
     */
    private suspend fun setVideoLike(aid: Long, like: Int): Boolean {
        val root = rawRequest(
            "/x/web-interface/archive/like",
            mapOf("aid" to aid.toString(), "like" to like.toString(), "csrf" to csrf),
            method = "POST",
            tolerateEmptyBody = true,
        )
        val code = root.optInt("code", 0)
        return when (code) {
            // 重复点赞 / 重复点踩 / 未点赞过 / 未点踩过：目标状态已经达成，按成功处理
            0, 65004, 65005, 65006, 65007 -> true
            else -> throw BiliException(code, apiMessage(code, root.optString("message")))
        }
    }

    suspend fun coinVideo(aid: Long, count: Int, alsoLike: Boolean): Boolean =
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

    suspend fun triple(aid: Long): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/web-interface/archive/like/triple",
            mapOf("aid" to aid.toString(), "csrf" to csrf),
        )
        true
    }

    /** 该视频已收藏在哪些收藏夹里（用于判断取消）。接口异常时返回空集合。 */
    suspend fun favoriteFolderIds(aid: Long): Set<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val root = rawRequest(
                "/x/v3/fav/resource/ids",
                mapOf("rid" to aid.toString(), "type" to "2"),
            )
            if (root.optInt("code", 0) != 0) return@runCatching emptySet()
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
    suspend fun setFavorite(aid: Long, folderId: Long, add: Boolean): Boolean =
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
    suspend fun removeFavoriteAll(aid: Long, folderIds: Collection<Long>): Boolean =
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
    suspend fun addWatchLater(aid: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            postRequest(
                "/x/v2/history/toview/add",
                mapOf("aid" to aid.toString(), "csrf" to csrf),
            )
        }.isSuccess
    }

    suspend fun favoriteVideo(aid: Long, folderIds: String): Boolean = withContext(Dispatchers.IO) {
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

    suspend fun followUser(mid: Long, follow: Boolean): Boolean = withContext(Dispatchers.IO) {
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
    suspend fun relation(mid: Long): Int? = withContext(Dispatchers.IO) {
        if (!isLoggedIn || mid <= 0) return@withContext null
        runCatching {
            dataRequest("/x/relation", mapOf("fid" to mid.toString()))
                .optInt("attribute", 0)
        }.getOrNull()
    }

    // ---------------------------------------------------------------- account

    suspend fun history(): List<HistoryItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/history/cursor",
            mapOf("max" to "0", "view_at" to "0", "business" to ""),
        )
        data.optJSONArray("list").objects().mapNotNull { item ->
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
            )
        }
    }

    /**
     * 云端观看进度：/x/player/v2 会返回登录账号在该视频上的 last_play_time。
     * 这是 B 站自己的续播数据，换设备/重装后仍然有效。
     * last_play_cid 和当前 cid 不一致时说明上次看的是别的分 P，不套用。
     */
    suspend fun cloudResumePositionMs(
        aid: Long,
        cid: Long,
        durationSec: Int = 0,
    ): Long = withContext(Dispatchers.IO) {
        if (aid <= 0 || cid <= 0 || !isLoggedIn) return@withContext 0L
        val data = runCatching {
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
    suspend fun reportHistory(
        aid: Long,
        cid: Long,
        progressSec: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        if (aid <= 0 || cid <= 0 || !isLoggedIn) return@withContext false
        runCatching {
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
    suspend fun enrichHistoryStats(items: List<HistoryItem>): List<HistoryItem> {
        if (items.isEmpty()) return items
        val missing = items.map { it.bvid }.distinct().filter { videoStatCache[it] == null }
        if (missing.isNotEmpty()) {
            val limiter = Semaphore(3)
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
                                    videoStatCache[bvid] = stat.l("view") to stat.l("danmaku")
                                }
                            }
                        }
                    }
                }
            }
        }
        return items.map { item ->
            val cached = videoStatCache[item.bvid]
            if (cached == null) {
                item
            } else {
                item.copy(play = cached.first.toInt(), danmaku = cached.second.toInt())
            }
        }
    }

    /** 稍后再看：/x/v2/history/toview，字段结构与历史记录接近。 */
    suspend fun watchLater(): List<HistoryItem> = withContext(Dispatchers.IO) {
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
            )
        }
    }

    suspend fun favoriteFolders(): List<FavoriteFolder> = withContext(Dispatchers.IO) {
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

    suspend fun favoriteVideos(folderId: Long): List<VideoItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/v3/fav/resource/list",
            mapOf(
                "media_id" to folderId.toString(),
                "pn" to "1",
                "ps" to "40",
                "order" to "mtime",
                "type" to "0",
                "tid" to "0",
                "platform" to "web",
            ),
        )
        data.optJSONArray("medias").objects().mapNotNull { media ->
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
    }

    suspend fun userDynamics(mid: Long): List<DynamicItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/polymer/web-dynamic/v1/feed/space",
            mapOf(
                "host_mid" to mid.toString(),
                "offset" to "",
                "page" to "1",
                "web_location" to "333.999",
                "features" to "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote",
            ),
            referer = "https://space.bilibili.com/$mid/dynamic",
        )
        data.optJSONArray("items").objects().mapNotNull { parseDynamicItem(it) }
    }

    /** UP 主投稿视频数。 */
    suspend fun upVideoCount(mid: Long): Long = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest("/x/space/navnum", mapOf("mid" to mid.toString()))
            data.l("video")
        }.getOrDefault(0L)
    }

    /** 当前同时观看人数：返回 (数字, 展示文案)。 */
    suspend fun onlineTotal(aid: Long, cid: Long): Pair<Long, String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val data = dataRequest(
                    "/x/player/online/total",
                    mapOf("aid" to aid.toString(), "cid" to cid.toString()),
                )
                data.optLong("count", 0L) to data.s("total")
            }.getOrDefault(0L to "")
        }

    suspend fun relationStat(mid: Long): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val data = dataRequest("/x/relation/stat", mapOf("vmid" to mid.toString()))
        (data.l("following") to data.l("follower"))
    }

    /** 用户获赞总数（官方空间统计）。 */
    suspend fun userLikes(mid: Long): Long = withContext(Dispatchers.IO) {
        runCatching {
            dataRequest("/x/space/upstat", mapOf("mid" to mid.toString())).l("likes")
        }.getOrDefault(0L)
    }

    suspend fun followings(): List<FollowedUser> = withContext(Dispatchers.IO) {
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

    /** 官方视频缩略图：拖动进度时的画面预览。 */
    suspend fun videoShot(cid: Long): VideoShot? = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/player/videoshot",
                mapOf("cid" to cid.toString(), "index" to "1"),
            )
            val imageArray = data.optJSONArray("image")
            val images = buildList {
                for (i in 0 until (imageArray?.length() ?: 0)) {
                    val raw = imageArray?.optString(i).orEmpty()
                    if (raw.isNotBlank()) add(if (raw.startsWith("//")) "https:$raw" else raw)
                }
            }
            val indexArray = data.optJSONArray("index")
            val times = buildList {
                for (i in 0 until (indexArray?.length() ?: 0)) add(indexArray?.optInt(i) ?: 0)
            }
            if (images.isEmpty() || times.isEmpty()) return@runCatching null
            VideoShot(
                images = images,
                cols = data.optInt("img_x_len", 1).coerceAtLeast(1),
                rows = data.optInt("img_y_len", 1).coerceAtLeast(1),
                frameWidth = data.optInt("img_x_size", 0),
                frameHeight = data.optInt("img_y_size", 0),
                timesSec = times,
            )
        }.getOrNull()
    }

    /** 视频字幕列表：`/x/player/v2` 的 `subtitle.subtitles`（含 AI 字幕）。 */
    suspend fun subtitles(aid: Long, cid: Long): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/player/v2",
                mapOf("aid" to aid.toString(), "cid" to cid.toString()),
            )
            val subtitle = data.optJSONObject("subtitle") ?: return@runCatching emptyList()
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
    suspend fun subtitleCues(url: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(if (url.startsWith("//")) "https:$url" else url)
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build()
            val text = client.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
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

    suspend fun dynamics(): List<DynamicItem> = dynamicPage("all").items

    /** 动态分页：type = all / video。 */
    suspend fun dynamicPage(type: String, offset: String = ""): DynamicPage =
        withContext(Dispatchers.IO) {
            val params = mutableMapOf(
                "timezone_offset" to "-480",
                "type" to type,
                "page" to "1",
                "features" to "itemOpusStyle",
            )
            if (offset.isNotBlank()) params["offset"] = offset
            val data = dataRequest("/x/polymer/web-dynamic/v1/feed/all", params)
            DynamicPage(
                items = data.optJSONArray("items").objects().mapNotNull { parseDynamicItem(it) },
                offset = data.s("offset"),
                hasMore = data.optBoolean("has_more", false),
            )
        }

    private fun parseDynamicItem(item: JSONObject, allowOrig: Boolean = true): DynamicItem? {
        val modules = item.optJSONObject("modules") ?: return null
        val author = modules.optJSONObject("module_author") ?: return null
        val dynamic = modules.optJSONObject("module_dynamic")
        val desc = dynamic?.optJSONObject("desc")?.s("text").orEmpty()
        val major = dynamic?.optJSONObject("major")
        val archive = major?.optJSONObject("archive")
        val opus = major?.optJSONObject("opus")
        val article = major?.optJSONObject("article")
        val common = major?.optJSONObject("common")
        val pgc = major?.optJSONObject("pgc")
        val live = major?.optJSONObject("live")
        val medialist = major?.optJSONObject("medialist")
        val courses = major?.optJSONObject("courses")
        val music = major?.optJSONObject("music")
        val ugcSeason = major?.optJSONObject("ugc_season")
        val video = archive?.let {
            VideoItem(
                bvid = it.s("bvid"),
                aid = it.l("aid"),
                title = it.s("title"),
                cover = image(it.s("cover"), 480),
                ownerName = author.s("name"),
                ownerFace = avatar(author.s("face"), 120),
                ownerMid = author.l("mid"),
                play = it.optJSONObject("stat")?.optString("play")?.toLongOrNull() ?: 0L,
                danmaku = it.optJSONObject("stat")?.optString("danmaku")?.toLongOrNull() ?: 0L,
                duration = parseDurationText(it.s("duration_text")),
                pubdate = 0,
                cid = 0,
            )
        } ?: ugcSeason?.let {
            VideoItem(
                bvid = it.s("bvid"),
                aid = it.l("aid"),
                title = it.s("title"),
                cover = image(it.s("cover"), 480),
                ownerName = author.s("name"),
                ownerFace = avatar(author.s("face"), 120),
                ownerMid = author.l("mid"),
                play = it.optJSONObject("stat")?.optString("play")?.toLongOrNull() ?: 0L,
                danmaku = it.optJSONObject("stat")?.optString("danmaku")?.toLongOrNull() ?: 0L,
                duration = parseDurationText(it.s("duration_text")),
                pubdate = 0,
                cid = 0,
            )
        }
        // 图文 opus：feed 在 itemOpusStyle 下把标题 / 正文放在 major.opus 里，
        // 只读 module_dynamic.desc 会出现“很多内容显示不出来”。
        val opusSummary = opus?.optJSONObject("summary")?.s("text").orEmpty()
        val title = opus?.s("title").orEmpty()
            .ifBlank { article?.s("title").orEmpty() }
            .ifBlank { common?.s("title").orEmpty() }
            .ifBlank { live?.s("title").orEmpty() }
            .ifBlank { medialist?.s("title").orEmpty() }
            .ifBlank { courses?.s("title").orEmpty() }
            .ifBlank { music?.s("title").orEmpty() }
        val text = desc
            .ifBlank { opusSummary }
            .ifBlank { article?.s("desc").orEmpty() }
            .ifBlank { common?.s("desc").orEmpty() }
            .ifBlank { common?.s("summary").orEmpty() }
            .ifBlank { pgc?.s("desc").orEmpty() }
            .ifBlank { medialist?.s("desc").orEmpty() }
            .ifBlank { music?.s("desc").orEmpty() }
        val images = buildList {
            major?.optJSONObject("draw")?.optJSONArray("items")?.objects()?.forEach {
                it.s("src").takeIf(String::isNotBlank)?.let { src -> add(image(src, 720)) }
            }
            opus?.optJSONArray("pics")?.objects()?.forEach {
                it.s("url").takeIf(String::isNotBlank)?.let { url -> add(image(url, 720)) }
            }
            article?.optJSONArray("covers")?.let { covers ->
                for (i in 0 until covers.length()) {
                    val value = covers.opt(i)
                    val url = when (value) {
                        is String -> value
                        is JSONObject -> value.s("url")
                        else -> ""
                    }
                    if (url.isNotBlank()) add(image(url, 720))
                }
            }
            common?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            live?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            medialist?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            courses?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            music?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        }.filter { it.isNotBlank() }.distinct()
        val stat = modules.optJSONObject("module_stat")
        val basic = item.optJSONObject("basic")
        val orig = if (allowOrig) {
            item.optJSONObject("orig")?.let { parseDynamicItem(it, allowOrig = false) }
        } else {
            null
        }
        return DynamicItem(
            id = item.s("id_str"),
            name = author.s("name"),
            face = avatar(author.s("face"), 120),
            mid = author.l("mid"),
            time = author.s("pub_time"),
            text = text,
            images = images,
            video = video,
            like = stat?.optJSONObject("like")?.optInt("count", 0) ?: 0,
            comment = stat?.optJSONObject("comment")?.optInt("count", 0) ?: 0,
            forward = stat?.optJSONObject("forward")?.optInt("count", 0) ?: 0,
            liked = stat?.optJSONObject("like")?.optBoolean("status", false) ?: false,
            commentOid = basic?.s("comment_id_str").orEmpty()
                .ifBlank { item.s("id_str") },
            commentType = (basic?.optInt("comment_type", 17) ?: 17)
                .takeIf { it > 0 } ?: 17,
            title = title,
            orig = orig,
            authorVip = author.optJSONObject("vip")?.optInt("vipType", 0) == 2,
            isCollection = !ugcSeason?.s("bvid").isNullOrBlank(),
            pgcCover = pgc?.s("cover").orEmpty().takeIf { it.isNotBlank() }
                ?.let { image(it, 720) }.orEmpty(),
            pgcTitle = pgc?.s("title").orEmpty(),
            pgcLabel = when (pgc?.optInt("sub_type", 0)) {
                3 -> "纪录片"
                1 -> "番剧"
                2 -> "电影"
                4 -> "国创"
                5 -> "电视剧"
                else -> "剧集"
            },
            ipLocation = author.s("pub_location_text")
                .ifBlank { author.s("pub_location") }
                .ifBlank { author.s("ip_location") }
                .ifBlank { author.s("location") },
        )
    }

    /**
     * 动态详情官方接口：feed 只给摘要时，打开详情用 detail 接口拿完整的
     * opus 正文 / 图片 / 转发内容。失败返回 null，由调用方回退到列表项。
     */
    suspend fun dynamicDetail(id: String): DynamicItem? = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/polymer/web-dynamic/v1/detail",
                mapOf(
                    "id" to id,
                    "dynamic_id" to id,
                    "platform" to "web",
                    "gaia_source" to "main_web",
                    "web_location" to "333.1368",
                    "features" to "itemOpusStyle,opusBigCover,onlyfansVote," +
                        "endFooterHidden,decorationCard,onlyfansAssetsV2,ugcDelete," +
                        "onlyfansQaCard,commentsNewVersion",
                ),
            )
            data.optJSONObject("item")?.let { parseDynamicItem(it) }
        }.getOrNull()
    }

    /** 动态点赞 / 取消。新接口要求 csrf 放 URL、正文是 JSON（form 会 4100001）。 */
    suspend fun likeDynamic(id: String, like: Boolean): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ensureBuvid()
            val jsonBody = JSONObject()
                .put("dyn_id_str", id)
                .put("up", if (like) 1 else 2)
                .toString()
            val request = Request.Builder()
                .url("$API/x/dynamic/feed/dyn/thumb?csrf=$csrf")
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .header("Origin", "https://www.bilibili.com")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            client.newCall(request).awaitResponse().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) throw BiliException(-1, "服务器返回为空")
                val json = JSONObject(body)
                val code = json.optInt("code", 0)
                if (code != 0) {
                    throw BiliException(code, apiMessage(code, json.optString("message")))
                }
            }
            true
        }.onFailure {
            android.util.Log.w("BiliApi", "likeDynamic failed id=$id like=$like", it)
        }.getOrDefault(false)
    }

    /** 动态详情评论（type=17）。 */
    suspend fun dynamicComments(id: String, page: Int = 1): CommentPage =
        withContext(Dispatchers.IO) {
            val data = dataRequest(
                "/x/v2/reply",
                mapOf(
                    "type" to "17",
                    "oid" to id,
                    "pn" to page.toString(),
                    "ps" to "20",
                    "sort" to "2",
                ),
            )
            val list = mutableListOf<CommentItem>()
            data.optJSONArray("top")?.objects()?.forEach { list += parseComment(it, 0, true) }
            data.optJSONArray("replies")?.objects()?.forEach { list += parseComment(it, 0, false) }
            val pageData = data.optJSONObject("page")
            val count = pageData?.optInt("count") ?: 0
            CommentPage(
                items = list.distinctBy { it.rpid },
                page = page,
                isEnd = page >= count,
                total = pageData?.optInt("acount") ?: 0,
            )
        }

    suspend fun sendDynamicComment(id: String, message: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                postRequest(
                    "/x/v2/reply/add",
                    mapOf(
                        "oid" to id,
                        "type" to "17",
                        "message" to message,
                        "plat" to "1",
                        "csrf" to csrf,
                    ),
                )
                true
            }.getOrDefault(false)
        }

    suspend fun space(mid: Long): SpaceInfo = withContext(Dispatchers.IO) {
        // 资料、关系、投稿、导航数互不依赖，并行拉取；页面总耗时从“四次相加”变成“最慢的一次”。
        val infoDeferred = async {
            dataRequest(
                "/x/space/wbi/acc/info",
                mapOf(
                    "mid" to mid.toString(),
                    "web_location" to "1550101",
                    "dm_img_list" to "[]",
                    "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                    "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEp",
                    "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}",
                ),
                signed = true,
                referer = "https://space.bilibili.com/$mid",
            )
        }
        val statDeferred = async {
            runCatching {
                dataRequest("/x/relation/stat", mapOf("vmid" to mid.toString()))
            }.getOrNull()
        }
        val videosDeferred = async {
            runCatching {
                dataRequest(
                    "/x/space/wbi/arc/search",
                    mapOf(
                        "mid" to mid.toString(),
                        "pn" to "1",
                        "ps" to "30",
                        "order" to "pubdate",
                        "web_location" to "1550101",
                        "dm_img_list" to "[]",
                        "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                        "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEp",
                        "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}",
                    ),
                    signed = true,
                    referer = "https://space.bilibili.com/$mid",
                )
            }.getOrNull()
        }
        val navDeferred = async {
            runCatching {
                dataRequest("/x/space/navnum", mapOf("mid" to mid.toString()))
            }.getOrNull()
        }

        val info = infoDeferred.await()
        val stat = statDeferred.await()
        val videos = videosDeferred.await()
        val nav = navDeferred.await()
        val vlist = videos?.optJSONObject("list")?.optJSONArray("vlist").objects().orEmpty()
        SpaceInfo(
            profile = UserProfile(
                mid = info.l("mid"),
                name = info.s("name"),
                face = avatar(info.s("face"), 200),
                sign = info.s("sign"),
                level = info.optInt("level", 0),
                coins = 0.0,
                vip = info.optJSONObject("vip")?.optInt("vipStatus") == 1,
                vipLabel = info.optJSONObject("vip")?.optJSONObject("label")?.s("text").orEmpty(),
                follower = stat?.l("follower") ?: 0,
                following = stat?.l("following") ?: 0,
                likes = info.optLong("likes", 0L).takeIf { it > 0L }
                    ?: runCatching {
                        dataRequest("/x/space/upstat", mapOf("mid" to mid.toString())).l("likes")
                    }.getOrDefault(0L).takeIf { it > 0L }
                    ?: nav?.l("likes") ?: 0L,
                banner = runCatching { image(info.s("top_photo"), 1080) }.getOrDefault(""),
                ipLocation = info.s("ip_location"),
                officialTitle = info.optJSONObject("official")?.s("title").orEmpty(),
                levelExp = info.optLong("level_exp", 0L),
                nextLevelExp = info.optLong("next_level_exp", 0L),
            ),
            videos = vlist.mapNotNull(::parseSpaceVideo),
            videoCount = nav?.optInt("video", 0) ?: vlist.size,
            isFollowed = info.optBoolean("is_followed", false),
        )
    }

    // ---------------------------------------------------------------- search

    /** B 站热搜词。依次尝试「搜索发现」和「热搜词」，都失败时给一份兜底列表。 */
    suspend fun hotKeywords(): List<String> = withContext(Dispatchers.IO) {
        // 1) 搜索发现：data.trending.list[].keyword
        val square = runCatching {
            val data = dataRequest(
                "/x/web-interface/search/square",
                mapOf("limit" to "10"),
                signed = true,
            )
            val list = data.optJSONObject("trending")?.optJSONArray("list")
            val out = ArrayList<String>()
            if (list != null) {
                for (i in 0 until list.length()) {
                    val word = list.optJSONObject(i)?.optString("keyword").orEmpty()
                    if (word.isNotBlank()) out.add(word)
                }
            }
            out
        }.getOrDefault(emptyList())

        if (square.isNotEmpty()) return@withContext square

        // 2) 热搜词接口
        runCatching {
            val root = rawRequest(
                path = "/main/hotword",
                params = emptyMap(),
                base = "https://s.search.bilibili.com",
            )
            val array = root.optJSONArray("data") ?: return@runCatching emptyList<String>()
            val result = ArrayList<String>(array.length())
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val word = item.optString("keyword").ifBlank { item.optString("name") }
                if (word.isNotBlank()) result.add(word)
            }
            result
        }.getOrDefault(emptyList())
    }

    suspend fun search(keyword: String, page: Int = 1): List<VideoItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/wbi/search/type",
            mapOf(
                "search_type" to "video",
                "keyword" to keyword,
                "page" to page.toString(),
                "page_size" to "20",
            ),
            signed = true,
        )
        data.optJSONArray("result").objects().mapNotNull { item ->
            val bvid = item.s("bvid")
            if (bvid.isBlank()) return@mapNotNull null
            VideoItem(
                bvid = bvid,
                aid = item.l("aid"),
                title = cleanHtml(item.s("title")),
                cover = image(item.s("pic"), 420),
                ownerName = cleanHtml(item.s("author")),
                ownerFace = "",
                ownerMid = item.l("mid"),
                play = item.s("play").toLongOrNull() ?: 0,
                danmaku = item.s("danmaku").toLongOrNull() ?: 0,
                duration = parseDurationText(item.s("duration")),
                pubdate = item.l("pubdate"),
            )
        }
    }

    suspend fun hotSearch(): List<String> = withContext(Dispatchers.IO) {
        val data = runCatching {
            dataRequest("/x/web-interface/search/square", mapOf("limit" to "10"))
        }.getOrNull() ?: return@withContext emptyList()
        data.optJSONObject("trending")?.optJSONArray("list").objects()
            ?.mapNotNull { it.s("keyword").takeIf(String::isNotBlank) }
            .orEmpty()
    }

    suspend fun messages(type: Int, page: Int = 1): List<MessageItem> = withContext(Dispatchers.IO) {
        val path = when (type) {
            0 -> "/x/msgfeed/reply"
            1 -> "/x/msgfeed/at"
            2 -> "/x/msgfeed/like"
            else -> "/x/msgfeed/system"
        }
        val data = dataRequest(path, mapOf("pn" to page.toString(), "ps" to "20"))
        data.optJSONArray("items").objects().mapNotNull { item ->
            val detail = item.optJSONObject("item")
            // 收到的赞把用户放在 users 数组里，其余类型是单个 user 对象
            val users = item.optJSONArray("users").objects()
            val user = item.optJSONObject("user") ?: users.firstOrNull()
            val names = users.mapNotNull {
                it.s("nickname").ifBlank { it.s("uname") }.takeIf(String::isNotBlank)
            }
            val detailType = detail?.s("type").orEmpty()
            val action = when {
                type == 1 || detailType == "at" -> "提到了我"
                type == 2 -> "赞了我的评论"
                detailType == "video" -> "对我的视频发表了评论"
                else -> "回复了我的评论"
            }
            val content = item.s("content")
                .ifBlank { detail?.s("content").orEmpty() }
                .ifBlank { detail?.s("message").orEmpty() }
            val source = detail?.s("source_content").orEmpty()
                .ifBlank { detail?.s("root_reply_content").orEmpty() }
                .ifBlank { detail?.s("target_reply_content").orEmpty() }
                .ifBlank { item.s("title") }
            val title = detail?.s("title").orEmpty().ifBlank { item.s("title") }
            if (title.isBlank() && content.isBlank() && source.isBlank()) return@mapNotNull null
            MessageItem(
                id = item.s("id").ifBlank { "${type}_${item.l("id")}" },
                title = title,
                content = content,
                time = item.l("reply_time")
                    .takeIf { it > 0 }
                    ?: item.l("ctime").takeIf { it > 0 }
                    ?: item.l("timestamp").takeIf { it > 0 }
                    ?: (item.s("pub_time").toLongOrNull() ?: 0L),
                name = user?.s("nickname").orEmpty().ifBlank { user?.s("uname").orEmpty() },
                avatar = avatar(user?.s("avatar").orEmpty(), 96),
                bvid = detail?.s("bvid").orEmpty(),
                action = action,
                source = source,
                userMid = user?.l("mid") ?: 0L,
                followed = user?.optBoolean("follow", false) ?: false,
                likeNames = names,
                likeCount = maxOf(
                    item.optInt("counts", 0),
                    detail?.optInt("counts", 0) ?: 0,
                    names.size,
                ),
            )
        }
    }

    /** 消息中心会话列表（系统通知 + 私信会话）。 */
    suspend fun messageSessions(followingOnly: Boolean): List<MessageSession> =
        withContext(Dispatchers.IO) {
            val request = mapOf(
                "session_type" to if (followingOnly) "9" else "4",
                "group_fold" to "0",
                "unfollow_fold" to "0",
                "sort_rule" to "2",
                "size" to "20",
                "mobi_app" to "web",
            )
            val root = rawRequest(
                "/session_svr/v1/session_svr/get_sessions",
                request,
                base = VC,
            )
            if (root.optInt("code", 0) != 0) return@withContext emptyList()
            val firstPage = root.optJSONObject("data")
                ?.optJSONArray("session_list")
                ?.objects()
                .orEmpty()
            val raw = if (firstPage.isEmpty() && followingOnly) {
                // session_type=9 在某些账号上会返回空，用全部会话按关注关系过滤兜底
                val fallback = rawRequest(
                    "/session_svr/v1/session_svr/get_sessions",
                    request + ("session_type" to "4"),
                    base = VC,
                )
                fallback.optJSONObject("data")
                    ?.optJSONArray("session_list")
                    ?.objects()
                    .orEmpty()
                    .filter {
                        it.optInt("is_follow", 0) == 1 &&
                            it.optInt("system_msg_type", 0) == 0
                    }
            } else {
                firstPage
            }
            val system = raw.map { it.toMessageSession() }
            // 私信会话只给 talker_id，名字/头像要单独补；最多补 12 个，避免一次打太多接口。
            val needResolve = system
                .filter { it.name.isBlank() && it.sessionType == 1 && !it.isSystem }
                .take(12)
            if (needResolve.isEmpty()) return@withContext system
            val resolved = kotlinx.coroutines.coroutineScope {
                needResolve.map { session ->
                    async {
                        val brief = userBrief(session.talkerId)
                        if (brief == null) {
                            session
                        } else {
                            session.copy(name = brief.first, avatar = brief.second)
                        }
                    }
                }.awaitAll()
            }.associateBy { it.id }
            system.map { resolved[it.id] ?: it }
        }

    /** 仅取用户昵称和头像，供私信会话补全。 */
    suspend fun userBrief(mid: Long): Pair<String, String>? = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/space/wbi/acc/info",
                mapOf("mid" to mid.toString()),
                signed = true,
            )
            val name = data.s("name")
            if (name.isBlank()) null else name to avatar(data.s("face"), 120)
        }.getOrNull()
    }

    /** 一键把所有未读消息标为已读；接口失败时由 UI 退化为本地清除红点。 */
    suspend fun clearAllUnread(sessions: List<MessageSession>): Boolean = withContext(Dispatchers.IO) {
        val msgfeed = listOf("/x/msgfeed/clearUnread", "/x/msgfeed/clear_unread").any { path ->
            runCatching {
                dataRequest(path, emptyMap(), method = "POST")
            }.isSuccess
        }
        val token = csrf
        val acked = sessions.filter { it.unread > 0 && it.maxSeqno > 0L }.map { session ->
            runCatching {
                dataRequest(
                    "/session_svr/v1/session_svr/update_ack",
                    mapOf(
                        "talker_id" to session.talkerId.toString(),
                        "session_type" to session.sessionType.toString(),
                        "ack_seqno" to session.maxSeqno.toString(),
                        "csrf" to token,
                        "csrf_token" to token,
                        "mobi_app" to "web",
                    ),
                    base = VC,
                    method = "POST",
                )
            }.isSuccess
        }.any { it }
        msgfeed || acked
    }

    /** 仅取用户 IP 属地，供动态详情在接口未返回发布 IP 时兜底。 */
    suspend fun userIpLocation(mid: Long): String = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/space/wbi/acc/info",
                mapOf(
                    "mid" to mid.toString(),
                    "web_location" to "1550101",
                ),
                signed = true,
                referer = "https://space.bilibili.com/$mid",
            )
            data.s("ip_location")
        }.getOrDefault("")
    }

    /** 兼容旧入口：只清会话未读。 */
    suspend fun markSessionsRead(sessions: List<MessageSession>): Boolean = withContext(Dispatchers.IO) {
        val token = csrf
        sessions.filter { it.unread > 0 && it.maxSeqno > 0L }.map { session ->
            runCatching {
                dataRequest(
                    "/session_svr/v1/session_svr/update_ack",
                    mapOf(
                        "talker_id" to session.talkerId.toString(),
                        "session_type" to session.sessionType.toString(),
                        "ack_seqno" to session.maxSeqno.toString(),
                        "csrf" to token,
                        "csrf_token" to token,
                        "mobi_app" to "web",
                    ),
                    base = VC,
                    method = "POST",
                )
            }.isSuccess
        }.any { it }
    }

    private fun JSONObject.toMessageSession(): MessageSession {
        val type = optInt("session_type", 1)
        val systemType = optInt("system_msg_type", 0)
        val account = optJSONObject("account_info")
        val name = account?.s("name").orEmpty()
            .ifBlank { if (type == 2) s("group_name") else "" }
        val avatar = account?.s("pic_url").orEmpty()
            .ifBlank { if (type == 2) s("group_cover") else "" }
        val last = optJSONObject("last_msg")
        return MessageSession(
            id = "${type}_${l("talker_id")}",
            talkerId = l("talker_id"),
            sessionType = type,
            name = name,
            avatar = avatar,
            preview = last?.let { previewOfMessage(it) }.orEmpty(),
            time = (l("session_ts") / 1_000_000L).takeIf { it > 0L }
                ?: (last?.l("timestamp") ?: 0L),
            unread = optInt("unread_count", 0),
            isSystem = systemType != 0 || account != null,
            maxSeqno = l("max_seqno"),
        )
    }

    private fun previewOfMessage(message: JSONObject): String {
        val type = message.optInt("msg_type", 0)
        val raw = message.optString("content")
        val obj = runCatching { JSONObject(raw) }.getOrNull()
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

    private fun parseFeedVideo(item: JSONObject): VideoItem? {
        val bvid = item.s("bvid")
        if (bvid.isBlank()) return null
        val owner = item.optJSONObject("owner")
        val stat = item.optJSONObject("stat")
        return VideoItem(
            bvid = bvid,
            aid = item.l("id").takeIf { it > 0 } ?: item.l("aid"),
            title = cleanHtml(item.s("title")),
            cover = image(item.s("pic"), 480),
            ownerName = owner?.s("name").orEmpty(),
            ownerFace = avatar(owner?.s("face").orEmpty(), 96),
            ownerMid = owner?.l("mid") ?: 0,
            play = stat?.l("view") ?: 0,
            danmaku = stat?.l("danmaku") ?: 0,
            duration = item.optInt("duration", 0),
            pubdate = item.l("pubdate"),
            cid = item.l("cid"),
            reason = item.optJSONObject("rcmd_reason")?.s("content").orEmpty(),
            isFollowed = item.optBoolean("is_followed", false),
        )
    }

    private fun parseSpaceVideo(item: JSONObject): VideoItem? {
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

    private fun parseVideoDetail(data: JSONObject): VideoDetail {
        val owner = data.optJSONObject("owner")
        val stat = data.optJSONObject("stat")
        val pages = data.optJSONArray("pages").objects().map { page ->
            VideoPage(
                cid = page.l("cid"),
                page = page.optInt("page", 1),
                part = page.s("part"),
                duration = page.optInt("duration", 0),
            )
        }
        val req = data.optJSONObject("req_user")
        val episodes = data.optJSONObject("ugc_season")
            ?.optJSONArray("sections").objects()
            ?.flatMap { section -> section.optJSONArray("episodes").objects() }
            ?.mapNotNull { ep ->
                val arc = ep.optJSONObject("arc")
                val bvid = ep.s("bvid").ifBlank { arc?.s("bvid").orEmpty() }
                val cid = ep.l("cid")
                if (bvid.isBlank() || cid <= 0) return@mapNotNull null
                VideoEpisode(
                    bvid = bvid,
                    aid = ep.l("aid").takeIf { it > 0 } ?: (arc?.l("aid") ?: 0L),
                    cid = cid,
                    title = ep.s("title").ifBlank { arc?.s("title").orEmpty() },
                    cover = image(arc?.s("pic").orEmpty(), 480),
                    duration = arc?.optInt("duration", 0) ?: 0,
                )
            }
            .orEmpty()
        return VideoDetail(
            bvid = data.s("bvid"),
            aid = data.l("aid"),
            title = cleanHtml(data.s("title")),
            cover = image(data.s("pic"), 1200),
            desc = data.s("desc"),
            ownerName = owner?.s("name").orEmpty(),
            ownerFace = avatar(owner?.s("face").orEmpty(), 160),
            ownerMid = owner?.l("mid") ?: 0,
            ownerSign = "",
            play = stat?.l("view") ?: 0,
            danmaku = stat?.l("danmaku") ?: 0,
            like = stat?.l("like") ?: 0,
            coin = stat?.l("coin") ?: 0,
            favorite = stat?.l("favorite") ?: 0,
            share = stat?.l("share") ?: 0,
            reply = stat?.l("reply") ?: 0,
            pubdate = data.l("pubdate"),
            duration = data.optInt("duration", 0),
            cid = data.l("cid"),
            tname = data.s("tname"),
            pages = pages,
            episodes = episodes,
            liked = req?.optInt("like", 0) == 1,
            coined = (req?.optInt("coin", 0) ?: 0) > 0,
            favorited = (req?.optInt("favorite", 0) ?: 0) > 0,
        )
    }

    private fun parsePlayInfo(
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
            val wanted = if (codecPreference == "auto") {
                emptyList()
            } else {
                reps.filter { codecMatches(it.s("codecs"), codecPreference) }
            }
            val avc = reps.filter { it.s("codecs").contains("avc", true) }
            val pool = wanted.ifEmpty { avc.ifEmpty { reps } }
            // 自动模式下不要盲目选最高码率：4K 常见 AV1/HEVC 两版，AV1 很多设备硬解不了，
            // 选错会触发 480P 兜底，表现出来就是“选了 4K 但很糊”。
            // 兼容性优先：AVC > HEVC > AV1 > 其他；同编码内再取最高码率。
            val rep = if (wanted.isNotEmpty()) {
                wanted.maxByOrNull { it.l("bandwidth") }
            } else {
                pool.sortedWith(
                    compareBy<JSONObject> { codecPriority(it.s("codecs")) }
                        .thenByDescending { it.l("bandwidth") },
                ).firstOrNull()
            } ?: return@forEach
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

    private fun chooseAudio(array: JSONArray?, cdn: CdnService): Pair<String, String>? {
        val audios = array.objects()
        if (audios.isEmpty()) return null
        val standard = audios.filter { it.optInt("id", 0) in setOf(30216, 30232, 30280) }
        val selected = standard.maxByOrNull { it.optInt("id", 0) } ?: audios.minByOrNull { it.l("bandwidth") }
        return selected?.let { audio ->
            val url = audio.s("baseUrl").ifBlank { audio.s("base_url") }
            cdn.rewrite(
                url,
                jsonStrings(audio.optJSONArray("backupUrl") ?: audio.optJSONArray("backup_url")),
            ) to url
        }
    }

    /** 自动选编码时的兼容性优先级，数字越小越优先。 */
    private fun codecPriority(codec: String): Int {
        val value = codec.lowercase()
        return when {
            value.contains("avc") || value.contains("h264") -> 0
            value.contains("hev") || value.contains("hvc") || value.contains("h265") -> 1
            value.contains("av01") || value.contains("av1") -> 2
            else -> 3
        }
    }

    private fun codecMatches(codec: String, preference: String): Boolean {
        val value = codec.lowercase()
        return when (preference.lowercase()) {
            "avc" -> value.contains("avc")
            "hevc" -> value.contains("hev") || value.contains("hvc")
            "av1" -> value.contains("av01") || value.contains("av1")
            "dvh1" -> value.contains("dvh1") || value.contains("dvhe") || value.contains("dolby")
            else -> true
        }
    }

    private fun jsonStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf { it.isNotBlank() }
        }
    }

    /** 给 CDN 设置测速用一个固定公开视频流。 */
    suspend fun cdnSampleUrl(): String? = withContext(Dispatchers.IO) {
        val data = runCatching {
            dataRequest(
                "/x/player/playurl",
                mapOf(
                    "bvid" to "BV1fK4y1t7hj",
                    "cid" to "196018899",
                    "qn" to "80",
                    "fnval" to "4048",
                    "fnver" to "0",
                    "fourk" to "1",
                ),
            )
        }.getOrNull() ?: return@withContext null
        data.optJSONObject("dash")
            ?.optJSONArray("video")
            ?.objects()
            ?.firstOrNull()
            ?.let { it.s("baseUrl").ifBlank { it.s("base_url") } }
            ?.takeIf { it.isNotBlank() }
    }

    private fun decodeDanmakuBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val decompressed = runCatching {
            when {
                bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte() -> {
                    GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                }
                bytes.size > 2 && bytes[0] == 0x78.toByte() -> {
                    InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                }
                bytes[0] == '<'.code.toByte() -> bytes
                else -> {
                    java.util.zip.InflaterInputStream(
                        ByteArrayInputStream(bytes),
                        java.util.zip.Inflater(true),
                    ).use { it.readBytes() }
                }
            }
        }.getOrNull()
        return String(decompressed ?: bytes, Charsets.UTF_8)
    }

    private fun parseDanmakuXml(xml: String): List<DanmakuItem> {
        if (xml.isBlank()) return emptyList()
        val result = ArrayList<DanmakuItem>(512)
        runCatching {
            val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(java.io.StringReader(xml))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && result.size < 4000) {
                if (event == XmlPullParser.START_TAG && parser.name == "d") {
                    val p = parser.getAttributeValue(null, "p").orEmpty()
                    val text = parser.nextText().orEmpty().trim()
                    val parts = p.split(',')
                    if (parts.size >= 4 && text.isNotBlank() && text.length <= 90) {
                        val seconds = parts[0].toFloatOrNull() ?: 0f
                        val mode = parts[1].toIntOrNull() ?: 1
                        if (mode == 1 || mode == 4 || mode == 5 || mode == 7 || mode == 8) {
                            result += DanmakuItem(
                                timeMs = (seconds * 1000).toLong(),
                                mode = mode,
                                color = parts[3].toIntOrNull() ?: 0xFFFFFF,
                                size = parts.getOrNull(2)?.toFloatOrNull() ?: 25f,
                                text = text,
                            )
                        }
                    }
                }
                event = parser.next()
            }
        }
        return result.sortedBy { it.timeMs }
    }

    private fun parseComment(
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
            mid = mid,
            oid = reply.l("oid"),
            root = reply.l("root"),
            parent = reply.l("parent"),
            emotes = emotes,
            ipLocation = reply.optJSONObject("reply_control")?.s("location").orEmpty(),
            previewReplies = if (depth < 1) {
                reply.optJSONArray("replies")?.objects()
                    ?.take(3)
                    ?.map { parseComment(it, upMid, false, depth + 1) }
                    .orEmpty()
            } else {
                emptyList()
            },
        )
    }

    // ---------------------------------------------------------------- request

    private class WbiKeys(val imgKey: String, val subKey: String) {
        val mixinKey: String = run {
            val raw = imgKey + subKey
            MIXIN_TABLE.map { raw.getOrElse(it) { '0' } }.joinToString("").take(32)
        }
    }

    private suspend fun ensureBuvid() {
        if (cookieJar.value("buvid3").isNotBlank()) return
        if (System.currentTimeMillis() < buvidRetryAt) return
        buvidMutex.withLock {
            if (cookieJar.value("buvid3").isNotBlank()) return
            if (System.currentTimeMillis() < buvidRetryAt) return
            val ok = try {
                val request = Request.Builder()
                    .url("$API/x/frontend/finger/spi")
                    .header("User-Agent", UA)
                    .header("Referer", "https://www.bilibili.com/")
                    .get()
                    .build()
                client.newCall(request).awaitResponse().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body?.string().orEmpty()
                    val data = JSONObject(body).optJSONObject("data")
                        ?: throw IOException("buvid data is empty")
                    val b3 = data.s("b_3")
                    if (b3.isBlank()) throw IOException("buvid3 is empty")
                    val b4 = data.s("b_4")
                    cookieJar.put("buvid3", b3)
                    if (b4.isNotBlank()) cookieJar.put("buvid4", b4)
                    cookieJar.put("b_nut", (System.currentTimeMillis() / 1000).toString())
                    true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("BiliApi", "buvid fetch failed", e)
                false
            }
            if (ok) {
                buvidRetryAt = 0L
            } else {
                // 失败后 30 秒内不再重试，否则每个接口都会先卡一次 buvid 请求。
                buvidRetryAt = System.currentTimeMillis() + 30_000L
            }
        }
    }

    private suspend fun ensureWbiKeys(): WbiKeys {
        val cached = wbiKeys
        if (cached != null &&
            System.currentTimeMillis() - wbiFetchedAt < WBI_TTL_MS
        ) {
            return cached
        }
        wbiMutex.withLock {
            val current = wbiKeys
            if (current != null &&
                System.currentTimeMillis() - wbiFetchedAt < WBI_TTL_MS
            ) {
                return current
            }
            val fallback = current
            try {
                val root = rawRequest("/x/web-interface/nav", emptyMap())
                parseWbi(root.optJSONObject("data")?.optJSONObject("wbi_img"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 刷新失败但旧 key 还在时继续用旧 key，别让整个推荐/搜索直接不可用。
                if (fallback != null) return fallback
                throw BiliException(-1, "无法获取请求签名，请检查网络")
            }
            return wbiKeys ?: fallback ?: throw BiliException(-1, "无法获取请求签名，请检查网络")
        }
    }

    private fun parseWbi(wbi: JSONObject?) {
        if (wbi == null) return
        val img = wbi.s("img_url").substringAfterLast('/').substringBefore('.')
        val sub = wbi.s("sub_url").substringAfterLast('/').substringBefore('.')
        if (img.isNotBlank() && sub.isNotBlank()) {
            wbiKeys = WbiKeys(img, sub)
            wbiFetchedAt = System.currentTimeMillis()
        }
    }

    private suspend fun dataRequest(
        path: String,
        params: Map<String, String>,
        signed: Boolean = false,
        base: String = API,
        method: String = "GET",
        referer: String? = null,
    ): JSONObject {
        val root = rawRequest(path, params, signed, base, method, referer)
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        return root.optJSONObject("data") ?: JSONObject()
    }

    private class RetryableHttpException(val statusCode: Int) : IOException("HTTP $statusCode")

    /** OkHttp 的异步回调转成可取消的 suspend，避免 execute() 占死 IO 线程且无法随页面取消。 */
    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            },
        )
        continuation.invokeOnCancellation { runCatching { cancel() } }
    }

    private fun retryDelayMs(attempt: Int): Long =
        350L * (1L shl attempt) + (0..180).random().toLong()

    private suspend fun executeOnce(
        request: Request,
        tolerateEmptyBody: Boolean,
    ): JSONObject = client.newCall(request).awaitResponse().use { response ->
        if (response.code == 408 || response.code == 429 || response.code >= 500) {
            throw RetryableHttpException(response.code)
        }
        if (response.code >= 400) {
            throw BiliException(response.code, "网络错误 HTTP ${response.code}")
        }
        val body = response.body?.string().orEmpty()
        if (body.isBlank()) {
            // 点赞/投币/收藏等动作接口成功时偶尔返回 2xx 空 body，不能当成“解析失败”
            if (tolerateEmptyBody && response.code in 200..299) return@use JSONObject()
            throw BiliException(-1, "服务器返回为空")
        }
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw BiliException(-1, "数据解析失败")
        }
        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                "BiliApi",
                "response ${request.url} http=${response.code} code=${json.optInt("code", 0)}",
            )
        }
        json
    }

    private suspend fun rawRequest(
        path: String,
        params: Map<String, String>,
        signed: Boolean = false,
        base: String = API,
        method: String = "GET",
        referer: String? = null,
        tolerateEmptyBody: Boolean = false,
    ): JSONObject = withContext(Dispatchers.IO) {
        ensureBuvid()
        val actualParams = params.toMutableMap()
        var url = "$base$path"
        if (signed) {
            val keys = ensureWbiKeys()
            actualParams["wts"] = (System.currentTimeMillis() / 1000).toString()
            val query = actualParams.toSortedMap().entries.joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value.replace(Regex("[!'()*]"), ""))}"
            }
            val rid = md5(query + keys.mixinKey)
            url = "$url?$query&w_rid=$rid"
        } else if (actualParams.isNotEmpty()) {
            val query = actualParams.entries.joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value)}"
            }
            url = "$url?$query"
        }
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", referer ?: if (base == PASSPORT) "https://passport.bilibili.com/" else "https://www.bilibili.com/")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
        if (method == "POST") {
            val form = FormBody.Builder()
            actualParams.forEach { (key, value) -> form.add(key, value) }
            builder.post(form.build())
                .header("Origin", "https://www.bilibili.com")
        }
        val request = builder.build()
        if (BuildConfig.DEBUG) android.util.Log.d("BiliApi", "request ${request.url}")

        // 只对 GET 做自动重试：POST 可能是投币/点赞，重复发送会有副作用。
        // 最多两次：一次正常、一次重试；避免网络完全断开时转圈太久。
        val attempts = if (method == "GET") 2 else 1
        var lastError: Exception? = null
        for (attempt in 0 until attempts) {
            try {
                return@withContext executeOnce(request, tolerateEmptyBody)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BiliException) {
                // 200 空 body / 解析失败也当作可重试的瞬时问题（仅 GET）。
                if (method == "GET" && e.code == -1 && attempt < attempts - 1) {
                    lastError = e
                } else {
                    throw e
                }
            } catch (e: RetryableHttpException) {
                lastError = BiliException(e.statusCode, "网络错误 HTTP ${e.statusCode}")
            } catch (e: IOException) {
                lastError = BiliException(-1, "网络连接失败，请检查网络后重试")
            } catch (e: Exception) {
                lastError = BiliException(-1, "网络连接失败，请检查网络后重试")
            }
            if (attempt < attempts - 1) delay(retryDelayMs(attempt))
        }
        val error = lastError ?: BiliException(-1, "网络连接失败，请检查网络后重试")
        if (BuildConfig.DEBUG) android.util.Log.w("BiliApi", "network error ${request.url}", error)
        throw error
    }

    private suspend fun postRequest(
        path: String,
        params: Map<String, String>,
        tolerateEmptyBody: Boolean = true,
    ): JSONObject {
        val result = rawRequest(
            path,
            params,
            method = "POST",
            tolerateEmptyBody = tolerateEmptyBody,
        )
        val code = result.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, result.optString("message")))
        return result.optJSONObject("data") ?: JSONObject()
    }

    companion object {
        const val API = "https://api.bilibili.com"
        const val PASSPORT = "https://passport.bilibili.com"
        const val VC = "https://api.vc.bilibili.com"
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

        /** WBI key 大约每天轮换；6 小时刷新一次足够，也避免进程活久了签名一直用旧 key。 */
        private const val WBI_TTL_MS = 6 * 60 * 60 * 1000L

        private val MIXIN_TABLE = intArrayOf(
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
        )

        private fun encode(value: String): String = Uri.encode(value, "-_.~")

        private fun md5(value: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }

        fun image(url: String, width: Int = 0): String {
            if (url.isBlank()) return ""
            var result = when {
                url.startsWith("//") -> "https:$url"
                url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
                else -> url
            }
            if (width > 0 && !result.contains("@")) {
                result += "@${width}w_${(width * 9 / 16)}h_1c.webp"
            }
            return result
        }

        fun avatar(url: String, size: Int): String {
            if (url.isBlank()) return ""
            var result = when {
                url.startsWith("//") -> "https:$url"
                url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
                else -> url
            }
            if (!result.contains("@")) {
                result += "@${size}w_${size}h_1c.webp"
            }
            return result
        }

        fun cleanHtml(text: String): String {
            if (text.isBlank()) return ""
            return runCatching {
                Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()
            }.getOrDefault(text.replace(Regex("<[^>]+>"), ""))
        }

        fun parseDurationText(text: String): Int {
            if (text.isBlank() || text == "0") return 0
            return runCatching {
                val parts = text.split(":")
                when (parts.size) {
                    3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                    2 -> parts[0].toInt() * 60 + parts[1].toInt()
                    else -> text.toIntOrNull() ?: 0
                }
            }.getOrDefault(0)
        }

        fun qualityLabel(id: Int): String = when (id) {
            127 -> "8K"
            126 -> "杜比视界"
            125 -> "HDR"
            120 -> "4K"
            116 -> "1080P60"
            112 -> "1080P+"
            80 -> "1080P"
            74 -> "720P60"
            64 -> "720P"
            32 -> "480P"
            16 -> "360P"
            else -> "${id}P"
        }
    }
}

internal fun JSONObject.s(key: String): String {
    if (isNull(key)) return ""
    return optString(key, "").takeIf { it != "null" } ?: ""
}

internal fun JSONObject.l(key: String): Long = optLong(key, 0L)

internal fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    val result = ArrayList<JSONObject>(length())
    for (i in 0 until length()) {
        optJSONObject(i)?.let(result::add)
    }
    return result
}

internal fun String.toReadableCount(): String {
    val value = toLongOrNull() ?: return this
    return when {
        value >= 100_000_000 -> "%.1f亿".format(value / 100_000_000.0)
        value >= 10_000 -> "%.1f万".format(value / 10_000.0)
        else -> value.toString()
    }
}

internal fun Int.toDurationText(): String {
    if (this <= 0) return "00:00"
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

internal fun Long.toAgoText(): String {
    if (this <= 0) return ""
    val diff = (System.currentTimeMillis() / 1000 - this).coerceAtLeast(0)
    return when {
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 86400 * 30 -> "${diff / 86400}天前"
        else -> {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
            date.format(java.util.Date(this * 1000))
        }
    }
}

internal fun String.toViewCount(): String = toReadableCount()

internal fun String.toDurationSeconds(): Int = BiliApi.parseDurationText(this)

internal fun Double.toCoinText(): String = if (this == this.toLong().toDouble()) this.toLong().toString() else "%.1f".format(this)
