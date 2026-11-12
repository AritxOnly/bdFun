package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun CommentThreadMenu(runtime: CommentThreadRuntime, input: CommentThreadInput) {
    val transient = runtime.transient
    val menuTarget = transient.menuReply
    TeakGlassMenu(
        visible = transient.menuVisible && menuTarget != null,
        onDismiss = { runtime.dismissMenu() }, anchorInWindow = transient.menuAnchor,
        minWidth = 168.dp, maxWidth = 216.dp, maxHeight = 420.dp,
    ) {
        if (menuTarget != null) {
            CommentThreadMenuBasicRows(runtime, input, menuTarget)
            CommentThreadMenuModerationRows(runtime, menuTarget)
        }
    }
}
