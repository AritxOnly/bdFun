package com.aritxonly.bdfun.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

import com.aritxonly.bdfun.core.application.VideoLinkText
import com.aritxonly.bdfun.core.application.collectRichLinks
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink

/**
 * 把正文里的链接染成主题色并变成可点击。
 * B 站视频链接用视频标题替换原文，点击打开 App 内视频页；
 * 其他链接仍显示原始地址并交给浏览器。
 */
internal fun linkifyText(
    text: String,
    linkColor: Color,
    titles: Map<String, ResolvedVideoLink> = emptyMap(),
    onOpenVideo: (String) -> Unit = {},
    onOpen: (String) -> Unit,
): AnnotatedString {
    val links = collectRichLinks(text)
    val sorted = links.sortedBy { it.start }
    if (sorted.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var index = 0
        for (link in sorted) {
            if (link.start < index) continue
            if (link.start > index) append(text.substring(index, link.start))
            val isVideo = link.video || VideoLinkText.isBiliToken(link.linkUrl.orEmpty())
            val label = if (isVideo) biliDisplayText(link.linkUrl.orEmpty(), titles) else link.linkUrl.orEmpty()
            withLink(
                LinkAnnotation.Clickable(
                    tag = "url-${link.start}",
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                    linkInteractionListener = {
                        if (isVideo) {
                            val bvid = titles[link.linkUrl.orEmpty()]?.bvid
                                ?: if (link.linkUrl.orEmpty().startsWith("BV", ignoreCase = true)) {
                                    "BV" + link.linkUrl.orEmpty().substring(2)
                                } else {
                                    null
                                }
                            if (bvid != null) onOpenVideo(bvid) else onOpen(link.linkUrl.orEmpty())
                        } else {
                            onOpen(link.linkUrl.orEmpty())
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
