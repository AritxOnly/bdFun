package com.aritxonly.bdfun.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.message.MessageItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LikeRow(item: MessageItem, onClick: () -> Unit = {}, onMore: () -> Unit = {}) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val summary = when {
        item.likeNames.size >= 2 ->
            "${item.likeNames[0]}、${item.likeNames[1]} 等总计${item.likeCount}人${item.action}"
        item.likeNames.size == 1 -> "${item.likeNames[0]} ${item.action}"
        else -> "有人${item.action}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onMore)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        MessageAvatar(item.avatar)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMessageDate(item.time),
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.weight(1f))
                MessageMoreDots(onMore)
            }
        }
        val likedText = item.subjectText
            .ifBlank { item.source }
            .ifBlank { item.content }
        if (item.videoSubject && item.subjectCover.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            MessageSubject(item)
        } else if (likedText.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            // 与「回复与@」右侧同一套灰色小字：显示被点赞的评论原文。
            Text(
                text = likedText,
                modifier = Modifier.width(84.dp),
                color = colors.onBackgroundVariant.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }
    }
}
