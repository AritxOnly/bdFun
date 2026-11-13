package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableStateOf
import com.aritxonly.bdfun.core.model.comment.CommentItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal data class CommentRowActions(
    val onLike: () -> Unit, val onDislike: () -> Unit, val onReply: () -> Unit,
    val onLongPress: (androidx.compose.ui.geometry.Rect) -> Unit,
    val onMore: (androidx.compose.ui.geometry.Rect) -> Unit,
    val onOpenReplies: () -> Unit, val onReplyToReply: (CommentItem) -> Unit,
    val onLongPressReply: (CommentItem, androidx.compose.ui.geometry.Rect) -> Unit,
    val onPreviewLike: (CommentItem) -> Unit,
)
internal data class CommentPreviewActions(
    val onOpen: () -> Unit, val onReplyToReply: (CommentItem) -> Unit,
    val onLongPressReply: (CommentItem, androidx.compose.ui.geometry.Rect) -> Unit,
    val onPreviewLike: (CommentItem) -> Unit,
)
internal class CommentRowPresentationStateHolder {
    var bounds by mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
}
