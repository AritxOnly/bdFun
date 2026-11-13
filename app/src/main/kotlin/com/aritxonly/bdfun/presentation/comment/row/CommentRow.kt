package com.aritxonly.bdfun.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import androidx.compose.runtime.getValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommentRow(item: CommentItem, disliked: Boolean, actions: CommentRowActions) {
    val likes = LocalVideoPageState.current.dependencies.comments.likes
    val ui = remember { CommentRowPresentationStateHolder() }
    // 只订阅这条评论自己的点赞状态；别的行被点赞时这里不会失效。
    val liked by remember(item.rpid, item.liked) {
        derivedStateOf { likes.likedMap[item.rpid] ?: item.liked }
    }
    val like by remember(item.rpid, item.like) {
        derivedStateOf { likes.countMap[item.rpid] ?: item.like }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                ui.bounds = it.boundsInWindow()
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = actions.onReply,
                onLongClick = { actions.onLongPress(ui.bounds) },
            )
            .padding(
                horizontal = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 6.dp else 12.dp,
                vertical = 10.dp,
            ),
    ) {
        CommentRowBody(item, disliked, liked, like, ui, actions)
    }
}
