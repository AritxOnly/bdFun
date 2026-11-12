package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentRowAvatar(item: CommentItem) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    AsyncImage(
        model = rememberImageRequest(item.avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = null, indication = null) {
                if (item.mid > 0L) {
                    com.aritxonly.bdfun.TeakNavigator.openSpace(context, item.mid)
                }
            },
    )
}
