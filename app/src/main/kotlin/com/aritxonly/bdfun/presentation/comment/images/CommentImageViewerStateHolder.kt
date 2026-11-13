package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal val LocalCommentImageViewer = androidx.compose.runtime.staticCompositionLocalOf<CommentImageViewerStateHolder> {
    error("评论图片浏览器必须位于评论页面内")
}

internal class CommentImageViewerStateHolder {
    var images by mutableStateOf<List<String>>(emptyList())
    var index by mutableIntStateOf(0)

    fun open(list: List<String>, at: Int) {
        images = list
        index = at.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    }

    fun close() {
        images = emptyList()
        index = 0
    }
}
