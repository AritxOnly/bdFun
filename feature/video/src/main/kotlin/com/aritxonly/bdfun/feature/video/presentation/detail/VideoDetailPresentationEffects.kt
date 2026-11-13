package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

@Composable
fun BindVideoFullscreenState(ui: VideoDetailUiStateHolder, publish: (Boolean) -> Unit, autoRotate: Boolean) {
    var fullscreen by ui.fullscreen
    var coinSheet by ui.coinSheet
    var favSheet by ui.favSheet
    // 独占全屏状态共享给 Activity；横屏分栏保留 tab / 回复栏。
    LaunchedEffect(fullscreen) { publish(fullscreen) }
    // 独占全屏时收回投币 / 收藏弹层。
    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            coinSheet = false
            favSheet = false
        }
    }
    // 横屏页面布局和播放器独占全屏分别管理。
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // 横屏宽度会把直板机也判成分栏；用不随旋转交换的最小宽度区分紧凑设备。
    val smallestWidthDp = configuration.smallestScreenWidthDp
    LaunchedEffect(landscape, smallestWidthDp) {
        com.aritxonly.bdfun.core.domain.fullscreenAfterVideoRotation(
            landscape, smallestWidthDp, autoRotate,
        )?.let { fullscreen = it }
    }
}

@Composable
fun ReconcileVideoPager(pager: androidx.compose.foundation.pager.PagerState) {
    // 兜底：极少数情况下 pager 停在上一次拖动的中间位置，
    // 松手后强制吸附到最近的一页，避免停在 0.3 / 0.7 这种位置。
    LaunchedEffect(pager) {
        snapshotFlow {
            pager.isScrollInProgress to pager.currentPageOffsetFraction
        }.collect { (scrolling, offset) ->
            if (!scrolling && kotlin.math.abs(offset) > 0.02f) {
                val target = (pager.currentPage + offset)
                    .roundToInt()
                    .coerceIn(0, pager.pageCount - 1)
                runCatching { pager.animateScrollToPage(target) }
            }
        }
    }

}
