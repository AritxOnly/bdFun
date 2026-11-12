package com.aritxonly.bdfun.ui

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 楼中楼里的浅色引用：左边一条竖线 + 「我：原文」。 */
@Composable
internal fun QuoteBlock(name: String, text: String) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(colors.onBackgroundVariant.copy(alpha = 0.22f)),
        )
        Spacer(Modifier.width(9.dp))
        EmoteText(
            content = EmoteTextContent(
                text = text,
                prefix = name,
            ),
            style = EmoteTextStyle(
                color = colors.onBackgroundVariant.copy(alpha = 0.82f),
                prefixColor = colors.onBackgroundVariant.copy(alpha = 0.9f),
                fontSize = 13.5.sp,
                lineHeight = 21.sp,
                maxLines = Int.MAX_VALUE,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun SmallMessageAction(icon: Int, label: String, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ResourceGlyph(res = icon, tint = colors.onBackgroundVariant, size = 14.dp)
        Spacer(Modifier.width(3.dp))
        Text(text = label, color = colors.onBackgroundVariant, fontSize = 11.sp)
    }
}

/** 三个圆点的省略号按钮：和评论区同款，圆底，不是方形文字块。 */
@Composable
internal fun MessageMoreDots(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = MiuixIcons.More,
            contentDescription = "更多",
            tint = colors.onBackgroundVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun MessageAvatar(url: String, size: androidx.compose.ui.unit.Dp = 44.dp) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(colors.surfaceContainerHigh),
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
