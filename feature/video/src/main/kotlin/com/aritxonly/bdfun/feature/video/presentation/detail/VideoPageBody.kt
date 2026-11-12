package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoDetail

@Composable
fun VideoPageBody(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder, playerBottomPx: () -> Float,
    detail: @Composable () -> Unit, comments: @Composable (VideoDetail, () -> Unit) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        detail()
        VideoPageFades(layout, playerBottomPx)
        var current by ui.comments
        val c = if (layout.pictureInPicture) null else current
        if (c != null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                comments(c) { current = null }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoPageFades(layout: VideoActivityLayout, playerBottomPx: () -> Float) {
    val pipMode = layout.pictureInPicture
    val pageFullscreen = layout.fullscreen
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    // 视频底边的白色渐变放进页面录制层，Tab 玻璃采样时会把它和下方内容
    // 当成同一张底片一起模糊，避免 Tab 内外一亮一白。
    val fadeColor = MaterialTheme.colorScheme.background
    if (!splitPanes) Box(
        modifier = Modifier
            .align(if (splitPanes) Alignment.TopEnd else Alignment.TopStart)
            .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
            .height(54.dp)
            .graphicsLayer {
                alpha = if (pipMode) 0f else pageOverlayAlpha
                // 渐变紧贴播放器真实底边。
                translationY = if (splitPanes) playerTopInset.toPx() else playerBottomPx()
            }
            .background(
                Brush.verticalGradient(
                    0f to fadeColor,
                    0.5f to fadeColor.copy(alpha = 0.94f),
                    0.8f to fadeColor.copy(alpha = 0.48f),
                    1f to fadeColor.copy(alpha = 0f),
                ),
            ),
    )
    // 页面底部反向渐变：顶部透明，越往下越接近页面底色。
    Box(
        modifier = Modifier
            .align(if (splitPanes) Alignment.BottomEnd else Alignment.BottomCenter)
            .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
            .height(54.dp)
            .graphicsLayer { alpha = if (pipMode) 0f else pageOverlayAlpha }
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    )
}
