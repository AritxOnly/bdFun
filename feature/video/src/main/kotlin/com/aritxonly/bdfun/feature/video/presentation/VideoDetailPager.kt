package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class VideoDetailPaneState(
    val fullscreen: Boolean, val pictureInPicture: Boolean, val splitPanes: Boolean,
    val paneLayout: VideoPaneLayout, val playerBlockHeight: androidx.compose.ui.unit.Dp,
    val statusBarTop: androidx.compose.ui.unit.Dp,
)
private class PortraitPaneStateHolder { var size by mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun androidx.compose.foundation.layout.BoxScope.VideoDetailPager(
    state: VideoDetailPaneState, pager: androidx.compose.foundation.pager.PagerState,
    collapseConnection: NestedScrollConnection, metadata: @Composable () -> Unit,
    tail: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
    comments: @Composable (androidx.compose.ui.unit.Dp) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val fullscreen = state.fullscreen
    val pictureInPicture = state.pictureInPicture
    val splitPanes = state.splitPanes
    val paneLayout = state.paneLayout
    val listState = rememberLazyListState()
    if (pager.currentPage == 0) TrackTeakPageScroll(listState)

    // 全屏时只隐藏、不卸载：否则退回竖屏评论列表会整段重载
    run {
        // 全屏时强制沿用竖屏尺寸，别让 LazyColumn 在横屏里重新测量、
        // 把底部已经组合好的「相关推荐」扔掉，退出全屏后又重新淡入一遍。
        val portrait = remember { PortraitPaneStateHolder() }
        val density = LocalDensity.current
        val freezePortrait = (fullscreen || pictureInPicture) &&
            portrait.size.width > 0 && portrait.size.height > 0
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(
                    if (freezePortrait) {
                        with(density) {
                            Modifier.size(
                                portrait.size.width.toDp(),
                                portrait.size.height.toDp(),
                            )
                        }
                    } else if (splitPanes) {
                        Modifier.width(paneLayout.detailsWidthDp.dp).fillMaxHeight()
                            .background(LocalTeakContentColors.current.background)
                    } else {
                        Modifier.fillMaxSize()
                    },
                )
                .onSizeChanged {
                    if (!fullscreen && !pictureInPicture) portrait.size = it
                }
                .graphicsLayer { alpha = if (fullscreen || pictureInPicture) 0f else 1f }
                .blockTouches(fullscreen || pictureInPicture),
        ) {
            VideoDetailPagerContent(state, pager, collapseConnection, listState, metadata, tail, comments, maxWidth)
        }
    }

}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun VideoDetailPagerContent(
    state: VideoDetailPaneState, pager: androidx.compose.foundation.pager.PagerState,
    collapseConnection: NestedScrollConnection, listState: androidx.compose.foundation.lazy.LazyListState,
    metadata: @Composable () -> Unit, tail: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
    comments: @Composable (Dp) -> Unit, commentsWidth: Dp,
) {
    val splitPanes = state.splitPanes
    val statusBarTop = state.statusBarTop
    val playerBlockHeight = state.playerBlockHeight
    HorizontalPager(
        state = pager,
        // 两页都保持 compose：切到简介再回来，评论列表不重载
        beyondViewportPageCount = 1,
        // 只有两页：到边后再往外滑不要任何回弹动画
        overscrollEffect = null,
        // 允许左右滑动；tab 直接读 pager.currentPage，会跟着一起动
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(collapseConnection),
    ) { page ->
        if (page == 0) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = (if (splitPanes) statusBarTop else playerBlockHeight) + 52.dp,
                    bottom = 32.dp,
                ),
            ) {
                item(key = "meta") { metadata() }
                tail()
            }
        } else {
            comments(commentsWidth)
        }
    }
}
