package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal class CommentImagePageStateHolder {
    var scale by androidx.compose.runtime.mutableFloatStateOf(1f)
    var pan by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
}

@Composable
internal fun CommentImagePage(url: String, viewer: CommentImageViewerStateHolder) {
    val ui = remember { CommentImagePageStateHolder() }
    AsyncImage(
        model = rememberImageRequest(url), contentDescription = null, contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().commentImageGestures(ui, viewer).graphicsLayer {
            scaleX = ui.scale
            scaleY = ui.scale
            translationX = ui.pan.x
            translationY = ui.pan.y
        },
    )
}
