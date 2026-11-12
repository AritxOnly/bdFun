package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
internal fun DynamicZoomableImage(url: String, dismissAnimated: () -> Unit) {
    val ui = remember { DynamicImageZoomStateHolder() }
    AsyncImage(
        model = rememberImageRequest(url),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .dynamicImageGestures(ui, dismissAnimated)
            .graphicsLayer {
                scaleX = ui.scale
                scaleY = ui.scale
                translationX = ui.pan.x
                translationY = ui.pan.y
            },
    )
}
