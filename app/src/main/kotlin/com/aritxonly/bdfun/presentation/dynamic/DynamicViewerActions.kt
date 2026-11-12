package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.feed.DynamicItem

@Composable
internal fun DynamicViewerActions(item: DynamicItem, liked: Boolean, onLike: () -> Unit, onComment: () -> Unit, onRepost: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        DynamicAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(TeakIcon.Share, Color.White, size = 20.dp)
            },
            count = item.forward,
            tint = Color.White,
            onClick = onRepost,
        )
        DynamicAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(TeakIcon.CommentBubble, Color.White, size = 20.dp)
            },
            count = item.comment,
            tint = Color.White,
            onClick = onComment,
        )
        DynamicAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(
                    if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                    if (liked) BrandPink else Color.White,
                    size = 20.dp,
                )
            },
            count = item.like,
            tint = if (liked) BrandPink else Color.White,
            onClick = onLike,
        )
    }
}
