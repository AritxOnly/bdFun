package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.message.MessageItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FanRow(
    item: MessageItem,
    followed: Boolean,
    busy: Boolean,
    onFollow: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessageAvatar(item.avatar)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name.ifBlank { "用户" },
                color = colors.onBackground,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "关注了你 · ${formatMessageDate(item.time)}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        FollowBackCapsule(
            followed = followed,
            busy = busy,
            onClick = onFollow,
        )
    }
}

@Composable
internal fun FollowBackCapsule(followed: Boolean, busy: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val shape = com.kyant.shapes.Capsule()
    Box(
        modifier = Modifier
            .width(74.dp)
            .height(30.dp)
            .clip(shape)
            .background(if (followed) colors.surfaceContainerHigh else BrandPink)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                busy -> "处理中"
                followed -> "已关注"
                else -> "+ 回关"
            },
            color = if (followed) colors.onBackgroundVariant else Color.White,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}
