package com.aritxonly.bdfun.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

/** 简介 / 评论里的普通链接：http(s):// 开头，遇到空白就结束。 */
internal val UrlPattern = Regex("https?://[^\\s\\u3000<>\"']+")

private val UrlTrailing = charArrayOf(
    '.', ',', ';', ':', '!', '?', ')', ']', '}',
    '，', '。', '！', '？', '、', '；', '：', '）', '】', '」', '』',
)

internal fun trimUrlTrailing(raw: String): String {
    var end = raw.length
    while (end > 0 && raw[end - 1] in UrlTrailing) end--
    return raw.substring(0, end)
}

/**
 * 把正文里的链接染成主题色并变成可点击。
 * B 站视频链接用视频标题替换原文，点击打开 App 内视频页；
 * 其他链接仍显示原始地址并交给浏览器。
 */
internal fun linkifyText(
    text: String,
    linkColor: Color,
    titles: Map<String, BiliLinkTitles.Resolved> = emptyMap(),
    onOpenVideo: (String) -> Unit = {},
    onOpen: (String) -> Unit,
): AnnotatedString {
    data class Link(val start: Int, val end: Int, val token: String, val video: Boolean)

    val links = mutableListOf<Link>()
    for (match in UrlPattern.findAll(text)) {
        val url = trimUrlTrailing(match.value)
        if (url.length <= "https://".length) continue
        links += Link(match.range.first, match.range.first + url.length, url, false)
    }
    // 单独的 BV 号 / av 号，以及被当成普通 URL 匹配到的 B 站链接。
    for (token in BiliLinkTitles.tokens(text)) {
        val start = text.indexOf(token)
        if (start < 0) continue
        val end = start + token.length
        if (links.any { it.start < end && start < it.end }) continue
        links += Link(start, end, token, true)
    }
    val sorted = links.sortedBy { it.start }
    if (sorted.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var index = 0
        for (link in sorted) {
            if (link.start < index) continue
            if (link.start > index) append(text.substring(index, link.start))
            val isVideo = link.video || BiliLinkTitles.isBiliToken(link.token)
            val label = if (isVideo) biliDisplayText(link.token, titles) else link.token
            withLink(
                LinkAnnotation.Clickable(
                    tag = "url-${link.start}",
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                    linkInteractionListener = {
                        if (isVideo) {
                            val bvid = titles[link.token]?.bvid
                                ?: if (link.token.startsWith("BV", ignoreCase = true)) {
                                    "BV" + link.token.substring(2)
                                } else {
                                    null
                                }
                            if (bvid != null) onOpenVideo(bvid) else onOpen(link.token)
                        } else {
                            onOpen(link.token)
                        }
                    },
                ),
            ) {
                append(label)
            }
            index = link.end
        }
        if (index < text.length) append(text.substring(index))
    }
}
