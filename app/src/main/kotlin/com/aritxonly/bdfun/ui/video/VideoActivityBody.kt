package com.aritxonly.bdfun.ui


import com.aritxonly.bdfun.ui.LocalVideoPageState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.CommentScreen
import com.aritxonly.bdfun.ui.VideoScreen
import kotlinx.coroutines.launch

@Composable
internal fun VideoActivityBody(
    isLoggedIn: () -> Boolean, settings: com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings,
    scene: VideoActivityScene, layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, scope: kotlinx.coroutines.CoroutineScope,
    host: VideoActivityHostActions,
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
    val loadedVideo = scene.video
    val commentRequest = scene.commentRequest
    var comments by ui.comments

    Box(modifier = Modifier.fillMaxSize()) {
        VideoScreen(
            isLoggedIn = isLoggedIn,
            request = com.aritxonly.bdfun.ui.VideoScreenRequest(
                video = loadedVideo,
                defaultQuality = settings.qualityPreference,
                openedFromVideoPage = host.openedFromVideoPage(),
                initialCommentRoot = commentRequest.first,
                initialCommentTarget = commentRequest.second,
                initialSeekMs = commentRequest.third,
            ),
            presentation = com.aritxonly.bdfun.ui.VideoScreenPresentation(
                paneLayout = paneLayout,
                pictureInPicture = pipMode,
                autoRotate = settings.autoRotate,
                immersiveStatusBar = settings.videoImmersiveStatusBar,
                // 从 Activity 根部读到的状态栏高度，避免页面内部读成 0。
                statusBarTop = statusBarDp,
            ),
            events = com.aritxonly.bdfun.ui.VideoScreenEvents(
                onOpenComments = {
                    if (paneLayout.isSplit) scope.launch { pager.animateScrollToPage(1) }
                    else comments = it
                },
                onBack = { host.onBack() },
            ),
            pager = pager,
        )
        VideoActivityFades(layout)
        VideoActivityComments( layout, ui)
    }

}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoActivityFades(layout: VideoActivityLayout) {
    val VideoPageBus = LocalVideoPageState.current
    val PlayerUiState = VideoPageBus.playerUi
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
                translationY = if (splitPanes) playerTopInset.toPx() else VideoPageBus.playerBottomPx
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

@Composable
private fun VideoActivityComments(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
) {
    val pipMode = layout.pictureInPicture
    var comments by ui.comments
    val c = if (pipMode) null else comments
    if (c != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            CommentScreen(
            request = CommentScreenRequest(
                aid = c.aid,
                upMid = c.ownerMid,
                title = c.title,
                bvid = c.bvid,
            ),
            onBack = { comments = null },
        )
        }
    }
}
