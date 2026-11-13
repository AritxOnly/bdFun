package com.aritxonly.bdfun.ui

import top.yukonga.miuix.kmp.icon.extended.More
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentRowMore(onMore: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = { onMore() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.More,
            contentDescription = "更多",
            tint = colors.onBackgroundVariant,
            modifier = Modifier.size(17.5.dp),
        )
    }
}
