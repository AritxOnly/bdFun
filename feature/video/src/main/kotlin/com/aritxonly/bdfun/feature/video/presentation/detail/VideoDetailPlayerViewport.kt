package com.aritxonly.bdfun.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun androidx.compose.foundation.layout.BoxScope.VideoDetailPlayerViewport(
    presentation: VideoScreenPresentation, inlineLayout: VideoInlineLayout, viewport: VideoPlayerViewport,
    pageUi: VideoDetailUiStateHolder, publishBottom: (Float) -> Unit, content: @Composable () -> Unit,
) {
    val fullscreen by pageUi.fullscreen
    val pictureInPicture = presentation.pictureInPicture
    val splitPanes = presentation.paneLayout.isSplit && !fullscreen && !pictureInPicture
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val playerDensity = LocalDensity.current
    androidx.compose.runtime.SideEffect {
        publishBottom(with(playerDensity) { playerBlockHeight.toPx() })
    }
    Box(modifier = videoDetailViewportModifier(presentation, inlineLayout, viewport, fullscreen, splitPanes)) {
        content()
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.videoDetailViewportModifier(
    presentation: VideoScreenPresentation, inlineLayout: VideoInlineLayout, viewport: VideoPlayerViewport,
    fullscreen: Boolean, splitPanes: Boolean,
): Modifier {
    val pictureInPicture = presentation.pictureInPicture
    val paneLayout = presentation.paneLayout
    val expandedTopInset = inlineLayout.expandedTopInset
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val playerTopInset = viewport.playerTopInset
    return if (fullscreen || pictureInPicture) {
        Modifier.fillMaxSize()
    } else if (splitPanes) {
        Modifier.align(Alignment.TopStart)
            .width(paneLayout.playerWidthDp.dp).fillMaxHeight()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(top = expandedTopInset)
            .clipToBounds()
    } else {
        Modifier
            .fillMaxWidth()
            // 黑色底就是播放器窗口本身：窗口收缩，画面跟着收缩，两者不会分离。
            // 状态栏那一条也要铺黑，否则沉浸时视频缩下去会露出白色页面底。
            .height(playerBlockHeight)
            .background(androidx.compose.ui.graphics.Color.Black)
            .padding(top = playerTopInset)
            .clipToBounds()
            .align(Alignment.TopCenter)
    }
}
