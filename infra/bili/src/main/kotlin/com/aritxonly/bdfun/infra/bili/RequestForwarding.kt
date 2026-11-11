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

internal suspend fun BiliApiContext.dataRequest(
    path: String,
    params: Map<String, String>,
    signed: Boolean = false,
    base: String = API,
    method: String = "GET",
    referer: String? = null,
    userAgent: String? = null,
    headers: Map<String, String> = emptyMap(),
): JSONObject =
    requests.dataRequest(path, params, signed, base, method, referer, userAgent, headers)

internal suspend fun BiliApiContext.rawRequest(
    path: String,
    params: Map<String, String>,
    signed: Boolean = false,
    base: String = API,
    method: String = "GET",
    referer: String? = null,
    tolerateEmptyBody: Boolean = false,
    userAgent: String? = null,
    headers: Map<String, String> = emptyMap(),
): JSONObject =
    requests.rawRequest(path, params, signed, base, method, referer, tolerateEmptyBody, userAgent, headers)

internal suspend fun BiliApiContext.postRequest(
    path: String,
    params: Map<String, String>,
    tolerateEmptyBody: Boolean = true,
): JSONObject =
    requests.postRequest(path, params, tolerateEmptyBody)

/**
 * 给 app.bilibili.com 的接口补上 App 签名。
 * 规则：params 按 key 排序 → 拼成 a=b&c=d → 末尾接 appsec → MD5。
 * 网页的 WBI 签名在这类接口上无效，会直接返回 -400。
 */

internal fun BiliApiContext.signAppParams(
    params: MutableMap<String, String>,
    appKey: String = APP_KEY,
    appSec: String = APP_SEC,
    includeAccessKey: Boolean = true,
) {
    appSigner.sign(
        params, appKey, appSec,
        if (includeAccessKey) cookieJar.value("access_key") else null,
    )
}
