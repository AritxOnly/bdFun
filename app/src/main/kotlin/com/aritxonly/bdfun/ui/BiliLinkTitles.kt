package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BiliLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 正文里的 B 站视频链接：识别 BV / av / bilibili.com/video / b23.tv，
 * 取到视频标题后用标题替换链接文字，点击直接打开 App 内视频页。
 */
object BiliLinkTitles {

    data class Resolved(val bvid: String, val title: String)

    private val tokenPattern = Regex(
        "(?i)" +
            "(https?://(?:www\\.|m\\.)?bilibili\\.com/video/(?:BV[0-9A-Za-z]{10}|av\\d+)[^\\s\\u3000<>\"']*)" +
            "|(https?://b23\\.tv/[0-9A-Za-z]+)" +
            "|((?<![0-9A-Za-z])BV[0-9A-Za-z]{10})" +
            "|((?<![0-9A-Za-z])av\\d{3,})",
    )

    private val cache = HashMap<String, Resolved>()
    private val failed = HashSet<String>()

    /** 按出现顺序返回去重后的原始片段（完整 URL 或裸 BV / av 号）。 */
    fun tokens(text: String): List<String> =
        tokenPattern.findAll(text)
            .map { trimUrlTrailing(it.value) }
            .distinct()
            .toList()

    fun cached(token: String): Resolved? = cache[token]

    fun isBiliToken(token: String): Boolean = tokenPattern.matches(token)

    suspend fun resolve(token: String): Resolved? {
        cache[token]?.let { return it }
        if (token in failed) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val bvid = when {
                    token.contains("b23.tv") -> BiliLink.resolve(token)
                    token.contains("bilibili.com/video/") ->
                        Regex("(?i)(BV[0-9A-Za-z]{10})").find(token)?.value
                    token.startsWith("BV", ignoreCase = true) -> "BV" + token.substring(2)
                    token.startsWith("av", ignoreCase = true) ->
                        token.substring(2).toLongOrNull()?.let { aid ->
                            AppGraph.api.viewByAid(aid).bvid
                        }
                    else -> null
                }
                val title = if (bvid.isNullOrBlank()) {
                    null
                } else {
                    AppGraph.api.view(bvid).title
                }
                if (bvid.isNullOrBlank() || title.isNullOrBlank()) {
                    null
                } else {
                    Resolved(bvid, title)
                }
            }.getOrNull()
        }?.also {
            cache[token] = it
        } ?: run {
            failed += token
            null
        }
    }
}

/**
 * 在当前正文里异步加载 B 站链接标题。
 * 返回的 map 是快照状态，标题到达后会触发文字重新组合。
 */
@Composable
fun rememberBiliLinkTitles(text: String): Map<String, BiliLinkTitles.Resolved> {
    val state = remember(text) { mutableStateMapOf<String, BiliLinkTitles.Resolved>() }
    LaunchedEffect(text) {
        val tokens = BiliLinkTitles.tokens(text)
        if (tokens.isEmpty()) return@LaunchedEffect
        // 先用进程缓存填上，避免每次进入都等网络。
        tokens.forEach { token ->
            BiliLinkTitles.cached(token)?.let { state[token] = it }
        }
        coroutineScope {
            tokens.filterNot { state.containsKey(it) }
                .map { token -> async { token to BiliLinkTitles.resolve(token) } }
                .awaitAll()
                .forEach { (token, resolved) ->
                    if (resolved != null) state[token] = resolved
                }
        }
    }
    // 读取快照，确保 map 变化时调用方重算 AnnotatedString。
    return state.toMap()
}

/** 视频链接显示的文字：标题还没到时先显示原始链接，避免文字突然变短。 */
fun biliDisplayText(
    token: String,
    resolved: Map<String, BiliLinkTitles.Resolved>,
): String = resolved[token]?.title ?: token
