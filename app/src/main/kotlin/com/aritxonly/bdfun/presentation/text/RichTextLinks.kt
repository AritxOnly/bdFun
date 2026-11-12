package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.isBiliVideoTextToken
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import com.aritxonly.bdfun.core.application.*

internal fun AnnotatedString.Builder.appendRichLink(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>, hit: RichTextHit) {
    val text = runtime.content.text
    val linkColor = runtime.linkColor
    val biliTitles = runtime.biliTitles
    val url = hit.linkUrl.orEmpty()
    val isVideo = isBiliVideoTextToken(url)
    val label = if (isVideo) biliDisplayText(url, biliTitles) else url
    withLink(
        androidx.compose.ui.text.LinkAnnotation.Clickable(
            tag = "url-${hit.start}",
            styles = androidx.compose.ui.text.TextLinkStyles(
                style = SpanStyle(color = linkColor),
            ),
            linkInteractionListener = {
                if (isVideo) {
                    val bvid = biliTitles[url]?.bvid
                        ?: if (url.startsWith("BV", ignoreCase = true)) {
                            "BV" + url.substring(2)
                        } else {
                            null
                        }
                    if (bvid != null) {
                        runtime.actions.onVideo(bvid)
                    } else {
                        runtime.actions.onUri(url)
                    }
                } else {
                    runtime.actions.onUri(url)
                }
            },
        ),
    ) {
        append(label)
    }

}

internal fun AnnotatedString.Builder.appendRichMention(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>, hit: RichTextHit) {
    val text = runtime.content.text
    val mentionColor = runtime.style.mentionColor
    val onMentionClick = runtime.actions.onMentionClick
    val mid = hit.mentionMid
    val name = hit.mentionName.orEmpty()
    withLink(
        androidx.compose.ui.text.LinkAnnotation.Clickable(
            tag = "mention-${hit.start}",
            styles = androidx.compose.ui.text.TextLinkStyles(
                style = SpanStyle(
                    color = mentionColor,
                    fontWeight = FontWeight.Normal,
                ),
            ),
            linkInteractionListener = { onMentionClick?.invoke(mid) },
        ),
    ) {
        append("@$name")
    }

}
