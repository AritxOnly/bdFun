package com.aritxonly.bdfun.ui


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
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import kotlinx.coroutines.launch

@Composable
fun androidx.compose.foundation.layout.BoxScope.VideoPageTabs(
    layout: VideoActivityLayout, playerBottomPx: () -> Float,
    pager: androidx.compose.foundation.pager.PagerState, onSelect: (Int) -> Unit,
    danmakuOn: Boolean, onOpenInput: () -> Unit, onToggleDanmaku: () -> Unit,
    chip: @Composable (String, Boolean, androidx.compose.ui.unit.Dp, () -> Unit) -> Unit,
) {
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    val overlayAlpha = pageOverlayAlpha
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
                    translationY = if (splitPanes) 0f else playerBottomPx()
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
                    onSelect = onSelect,
                    itemWidth = if (splitPanes) 48.dp else 60.dp,
                )
                VideoDanmakuActions(splitPanes, danmakuOn, onOpenInput, onToggleDanmaku, chip)
            }
        }
    }

}

@Composable
private fun RowScope.VideoDanmakuActions(
    compact: Boolean, danmakuOn: Boolean, onOpenInput: () -> Unit, onToggleDanmaku: () -> Unit,
    chip: @Composable (String, Boolean, androidx.compose.ui.unit.Dp, () -> Unit) -> Unit,
) {
    Spacer(Modifier.weight(1f))
    // 点我发弹幕（与「简介/评论」同一套玻璃）
    chip("点我发弹幕", false, if (compact) 10.dp else 14.dp, onOpenInput)
    Spacer(Modifier.width(8.dp))
    // 「弹」沿用app的原真玻璃组合，开关状态与动作显式输入。
    chip("弹", danmakuOn, if (compact) 10.dp else 15.dp, onToggleDanmaku)
}
