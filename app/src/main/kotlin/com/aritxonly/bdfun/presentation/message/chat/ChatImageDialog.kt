package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.aritxonly.bdfun.ui.rememberImageRequest

internal val LocalChatImageOpen = staticCompositionLocalOf<(String) -> Unit> { {} }
private class ChatImageTransform {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
}

@Composable
internal fun ChatImageDialog(url: String?, onDismiss: () -> Unit) {
    if (url == null) return
    val ui = remember(url) { ChatImageTransform() }
    val transform = rememberTransformableState { zoom, pan, _ ->
        ui.scale = (ui.scale * zoom).coerceIn(1f, 4f)
        ui.offset = if (ui.scale > 1f) ui.offset + pan else Offset.Zero
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AsyncImage(rememberImageRequest(url), "私信图片，可双指缩放", contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().background(Color.Black).transformable(transform)
                .graphicsLayer { scaleX = ui.scale; scaleY = ui.scale; translationX = ui.offset.x; translationY = ui.offset.y })
    }
}
