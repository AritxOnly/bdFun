package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.message.MessageItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 消息右侧的「被回复 / 被赞」对象：视频放封面，评论放原文。 */
@Composable
internal fun MessageSubject(item: MessageItem) {
    val colors = MiuixTheme.colorScheme
    // 右侧对象统一用正方形区域；缩到 60dp 给左边正文留空间。
    val size = 60.dp
    if (item.videoSubject) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            if (item.subjectCover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(item.subjectCover),
                    contentDescription = if (item.title.isNotBlank()) item.title else "视频封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    color = colors.onBackgroundVariant,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.size(size),
        ) {
            Text(
                text = item.subjectText,
                modifier = Modifier.fillMaxSize(),
                color = colors.onBackgroundVariant.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }
    }
}
