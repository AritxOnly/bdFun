package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoScreenLayout(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    events: VideoScreenEvents, pager: PagerState, inlineLayout: VideoInlineLayout,
    holders: VideoScreenStateHolders, content: VideoScreenContent,
    actions: VideoDetailViewActions, relatedEnter: Float, scope: CoroutineScope,
) {
    val background = LocalVideoPageState.current.dependencies.background
    val pageUi = holders.ui
    val fullscreen by pageUi.fullscreen
    val playing by pageUi.playing
    val colors = MiuixTheme.colorScheme
    val loading = content.read.loading
    val pictureInPicture = presentation.pictureInPicture
    val paneLayout = presentation.paneLayout
    val immersiveStatusBar = presentation.immersiveStatusBar
    val statusBarTop = presentation.statusBarTop
    val statusTop = statusBarTop
    val splitPanes = paneLayout.isSplit && !fullscreen && !pictureInPicture
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val expandedPlayerHeight = inlineLayout.expandedPlayerHeight
    val collapsedPlayerHeight = inlineLayout.collapsedPlayerHeight
    val animatedPlayerHeight = inlineLayout.animatedPlayerHeight
    val collapseState = inlineLayout.collapseState
    val description = holders.description
    val metadataController = holders.metadata
    Box(modifier = Modifier.fillMaxSize()) {
        VideoScreenPager( request, presentation, pager, inlineLayout, pageUi, description,
            content,
            actions, relatedEnter)

        if (loading) {
            TeakLoading(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
                    .size(28.dp),
                color = colors.primary,
            )
        }

        val viewport = resolveVideoPlayerViewport(VideoViewportInput(
            expandedPlayerHeight, collapsedPlayerHeight, animatedPlayerHeight, statusTop, immersiveStatusBar,
            fullscreen, pictureInPicture, splitPanes,
        ))
        VideoDetailPlayerWindow(
            content,
            presentation, events, inlineLayout, viewport, pageUi, actions, metadataController, scope,
        )

        VideoCollapseOverlay(fullscreen, viewport.collapseProgress, playerBlockHeight, statusTop,
            com.aritxonly.bdfun.BrandPink, TeakIcon.PlayerPlay) {
            collapseState.restore()
            if (!playing) background.togglePlayPause()
        }
    }

}
