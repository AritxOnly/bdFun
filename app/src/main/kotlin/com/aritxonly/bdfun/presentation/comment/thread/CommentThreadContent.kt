package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.CommentThreadContent(render: CommentThreadRender) {
    val progress = render.layout.progress
    val background = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxSize()
            .graphicsLayer {
                // 从下往上拉起，到位即停，没有弹簧回弹。
                val t = progress.coerceIn(0f, 1f)
                translationY = (1f - t) * size.height
            }
            .background(background),
    ) {
        CommentThreadPane(render)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CommentThreadPane(render: CommentThreadRender) {
    val transient = render.transient
    val callbacks = render.callbacks
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        val headerHeight = 46.dp
        CommentThreadList(render)

        CommentThreadFades(background, headerHeight, transient, callbacks)
    }
}
