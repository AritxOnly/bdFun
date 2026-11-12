package com.aritxonly.bdfun.ui

import top.yukonga.miuix.kmp.icon.extended.More
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem

@Composable
internal fun CommentRowFooter(
    item: CommentItem, disliked: Boolean, liked: Boolean, like: Int,
    ui: CommentRowPresentationStateHolder, actions: CommentRowActions,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommentRowMetadata(item, actions.onReply)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        // 点赞：整体略上移
        CommentRowLike(liked, like, actions.onLike)
        Spacer(Modifier.width(10.dp))
        // 点踩：整体略下移
        CommentRowDislike(disliked, actions.onDislike)
        Spacer(Modifier.width(10.dp))
        // 省略号：直接使用 Miuix 的 More 图标，尺寸和返回键箭头一致。
        CommentRowMore { actions.onMore(ui.bounds) }
    }
}
