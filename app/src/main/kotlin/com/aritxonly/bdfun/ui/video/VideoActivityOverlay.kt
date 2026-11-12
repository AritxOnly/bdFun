package com.aritxonly.bdfun.ui


import com.aritxonly.bdfun.ui.LocalVideoPageState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.CommentReplyBar
import com.aritxonly.bdfun.ui.DanmakuInputBar
import com.aritxonly.bdfun.ui.DanmakuSettingsPanel
import com.aritxonly.bdfun.ui.rememberSpringFloat
import com.aritxonly.bdfun.ui.blockTouches
import com.aritxonly.bdfun.ui.rememberManualProgress

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoActivityOverlay(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, scope: kotlinx.coroutines.CoroutineScope,
) {
    if (layout.pictureInPicture) return
    val page = LocalVideoPageState.current
    LaunchedEffect(layout.fullscreen) {
        if (layout.fullscreen) {
            page.dismissReply++
            ui.danmakuInputOpen.value = false
        }
    }
    if (layout.overlayAlpha > 0.01f) {
        VideoActivityTabs(layout, ui, pager, scope)
        VideoActivityReplyLayer(layout, pager)
        VideoActivityInputLayer(layout, ui)
    }
    VideoActivityDanmakuPanel(layout)
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoActivityReplyLayer(
    layout: VideoActivityLayout, pager: androidx.compose.foundation.pager.PagerState,
) {
    val VideoPageBus = LocalVideoPageState.current
    val PlayerUiState = VideoPageBus.playerUi
    val pipMode = layout.pictureInPicture
    val pageFullscreen = layout.fullscreen
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    val fs = pageFullscreen
    // 评论回复栏属于详情栏；只有独占全屏才隐藏。
    if (!fs && pager.currentPage == 1) {
        // 编辑中：铺一层全屏点击层，点空白处直接退出（同时杜绝点击穿透到播放器）
        if (VideoPageBus.replyEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val down = event.changes.firstOrNull { it.pressed }
                                if (down != null) {
                                    VideoPageBus.dismissReply++
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }
        val replyBarAlpha = rememberManualProgress(
            if (VideoPageBus.commentMenuOpen) 0f else 1f,
            180f,
        )
        if (replyBarAlpha > 0.01f) {
            CommentReplyBar(
                modifier = Modifier
                    .align(if (splitPanes) Alignment.BottomEnd else Alignment.BottomCenter)
                    .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier)
                    .graphicsLayer { alpha = replyBarAlpha }
                    .blockTouches(replyBarAlpha < 0.99f),
            )
        }
    }

}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoActivityInputLayer(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
) {
    val VideoPageBus = LocalVideoPageState.current
    val PlayerUiState = VideoPageBus.playerUi
    val pipMode = layout.pictureInPicture
    val pageFullscreen = layout.fullscreen
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    val fs = pageFullscreen
    var danmakuInputOpen by ui.danmakuInputOpen
    // 发弹幕输入
    if (danmakuInputOpen && !fs) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val down = event.changes.firstOrNull { it.pressed }
                            if (down != null) VideoPageBus.danmakuInputClose++
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        )
        DanmakuInputBar(
            onDismiss = { danmakuInputOpen = false },
            modifier = Modifier
                .align(if (splitPanes) Alignment.BottomStart else Alignment.BottomCenter)
                .then(if (splitPanes) Modifier.width(paneLayout.playerWidthDp.dp) else Modifier),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoActivityDanmakuPanel(layout: VideoActivityLayout) {
    val VideoPageBus = LocalVideoPageState.current
    val PlayerUiState = VideoPageBus.playerUi
    val pipMode = layout.pictureInPicture
    val pageFullscreen = layout.fullscreen
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    // 弹幕设置面板：统一玻璃 + 弹簧弹出；动画过程中面板自身由模糊变清晰
    val panelOpen = PlayerUiState.danmakuPanelOpen
    val spring = rememberSpringFloat(
        target = if (panelOpen) 1f else 0f,
        stiffness = 300f,
        damping = 22f,
    )
    val shown = spring.coerceIn(0f, 1f)
    if (panelOpen || shown > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                PlayerUiState.danmakuPanelOpen = false
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        )
        val selfBlur = if (shown < 0.99f) {
            Modifier.blur((10f * (1f - shown)).dp)
        } else {
            Modifier
        }
        DanmakuSettingsPanel(
            onInteract = { PlayerUiState.danmakuInteract++ },
            backdrop = VideoPageBus.panelBackdrop,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarDp + 54.dp, end = if (splitPanes) paneLayout.detailsWidthDp.dp + 12.dp else 12.dp)
                .graphicsLayer {
                    alpha = shown
                    scaleX = 0.82f + 0.18f * spring
                    scaleY = 0.82f + 0.18f * spring
                    translationY = (1f - shown) * -14f
                    transformOrigin =
                        androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                }
                .then(selfBlur),
        )
    }

}
