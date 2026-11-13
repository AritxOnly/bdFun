package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp

@Composable
internal fun CommentImageDialog() {
    val CommentImageViewer = LocalCommentImageViewer.current


    val images = CommentImageViewer.images
    if (images.isEmpty()) return
    val pager = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = CommentImageViewer.index.coerceIn(0, images.lastIndex),
    ) { images.size }
    androidx.activity.compose.BackHandler { CommentImageViewer.close() }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { CommentImageViewer.close() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                CommentImagePage(images[page], CommentImageViewer)
            }
            Text(
                text = "${pager.currentPage + 1}/${images.size}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
            )
        }
    }
}
