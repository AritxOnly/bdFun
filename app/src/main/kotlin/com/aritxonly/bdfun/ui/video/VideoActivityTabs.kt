package com.aritxonly.bdfun.ui


import com.aritxonly.bdfun.ui.LocalVideoPageState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.TeakGlassChip
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import kotlinx.coroutines.launch

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoActivityTabs(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, scope: kotlinx.coroutines.CoroutineScope,
) {
    val VideoPageBus = LocalVideoPageState.current
    val PlayerUiState = VideoPageBus.playerUi
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    val overlayAlpha = pageOverlayAlpha
    var danmakuInputOpen by ui.danmakuInputOpen
    androidx.compose.runtime.CompositionLocalProvider(
        com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides
            com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current.materialProgress,
    ) {
        com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial(
            enabled = splitPanes,
            modifier = Modifier
                .align(if (splitPanes) Alignment.TopEnd else Alignment.TopStart)
                .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
                .graphicsLayer {
                    alpha = overlayAlpha
                    translationY = if (splitPanes) 0f else VideoPageBus.playerBottomPx
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = if (splitPanes) statusBarDp else 0.dp)
                    .height(52.dp)
                    .padding(horizontal = if (splitPanes) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeakPagerTabs(
                    options = listOf("简介", "评论"), selected = pager.currentPage,
                    onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                    itemWidth = if (splitPanes) 48.dp else 60.dp,
                )
                VideoDanmakuActions(compact = splitPanes) { danmakuInputOpen = true }
            }
        }
    }

}

@Composable
internal fun RowScope.VideoDanmakuActions(compact: Boolean = false, onOpenInput: () -> Unit) {
    val settings = LocalVideoPageState.current.settings
    val VideoPageBus = LocalVideoPageState.current

    Spacer(Modifier.weight(1f))
    // 点我发弹幕（与「简介/评论」同一套玻璃）
    TeakGlassChip(
        label = "点我发弹幕",
        horizontalPadding = if (compact) 10.dp else 14.dp,
        active = false,
        onClick = onOpenInput,
    )
    Spacer(Modifier.width(8.dp))
    // 「弹」→ 开关弹幕显示：开启时粉色玻璃 + 白字，关闭时灰字白玻璃
    TeakGlassChip(
        label = "弹",
        active = settings.danmakuOn,
        horizontalPadding = if (compact) 10.dp else 15.dp,
        onClick = {
            settings.danmakuOn = !settings.danmakuOn
            VideoPageBus.danmakuOn = settings.danmakuOn
        },
    )
}
