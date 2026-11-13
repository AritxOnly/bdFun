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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.message.MessageItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/* ------------------------------------------------ 消息卡片：回复 / 喜欢 / 粉丝 */

@Composable
internal fun ReplyRow(
    item: MessageItem,
    myName: String,
    onClick: () -> Unit = {},
    onMore: () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onMore)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // 头像缩小 20%，把宽度让给回复正文。
        MessageAvatar(item.avatar, size = 35.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.onBackground)) {
                            append(item.name.ifBlank { "用户" })
                        }
                        append(" ")
                        withStyle(SpanStyle(color = colors.onBackgroundVariant)) {
                            append(item.action)
                        }
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }
            if (item.content.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                EmoteText(
                    content = EmoteTextContent(
                        text = item.content,
                    ),
                    style = EmoteTextStyle(
                        color = colors.onBackground,
                        fontSize = 14.85.sp,
                        lineHeight = 23.sp,
                        maxLines = Int.MAX_VALUE,
                    ),
                )
            }
            if (item.quote.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                QuoteBlock(
                    name = myName.ifBlank { "我" },
                    text = item.quote,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMessageDate(item.time),
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(12.dp))
                SmallMessageAction(TeakIcon.CommentBubble, "回复", onClick)
                Spacer(Modifier.width(12.dp))
                SmallMessageAction(TeakIcon.LikeOutline, "查看评论", onClick)
                Spacer(Modifier.weight(1f))
                MessageMoreDots(onMore)
            }
        }
        if (item.videoSubject || item.subjectText.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            MessageSubject(item)
        }
    }
}
