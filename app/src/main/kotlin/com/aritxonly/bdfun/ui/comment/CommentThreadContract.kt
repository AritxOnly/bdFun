package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.core.model.comment.CommentItem

internal data class CommentThreadInput(
    val visible: Boolean, val comment: CommentItem, val replies: List<CommentItem>,
    val loading: Boolean, val end: Boolean, val commentOid: String, val commentType: Int,
    val bvid: String, val videoTitle: String, val sortHot: Boolean,
)
internal data class CommentThreadViewport(
    val focusRpid: Long, val minTopPx: Float, val playerBottomDp: Dp?, val paneWidth: Dp?,
)
internal class CommentThreadNavigation(
    val onToggleSort: () -> Unit, val onLoadMore: () -> Unit, val onDismiss: () -> Unit,
)
