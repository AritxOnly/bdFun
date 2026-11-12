package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.aritxonly.bdfun.core.model.feed.DynamicItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun DynamicImageViewer(
    item: DynamicItem, url: String, origin: Rect?, liked: Boolean,
    onDismiss: () -> Unit, onLike: () -> Unit, onComment: () -> Unit, onRepost: () -> Unit,
) {
    val ui = remember { DynamicImageViewerStateHolder() }
    LaunchedEffect(Unit) { ui.shown = true }
    val scope = rememberCoroutineScope()
    val dismissAnimated: () -> Unit = {
        if (ui.shown) {
            ui.shown = false
            scope.launch {
                delay(200)
                onDismiss()
            }
        } else {
            onDismiss()
        }
        Unit
    }
    val entrance = rememberDynamicImageEntrance(ui.shown, origin)
    val images = remember(item.images, url) {
        item.images.ifEmpty { listOf(url) }
    }
    val imagePager = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = images.indexOf(url).coerceAtLeast(0).coerceAtMost(images.lastIndex),
        pageCount = { images.size },
    )
    androidx.compose.ui.window.Dialog(
        onDismissRequest = dismissAnimated,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = entrance.alpha))) {
            DynamicViewerTopBar(dismissAnimated)
            DynamicViewerPager(imagePager, images, entrance, dismissAnimated)
            DynamicViewerFooter(item, liked, dismissAnimated, onLike, onComment, onRepost)
        }
    }
}
