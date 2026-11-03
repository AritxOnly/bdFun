package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 状态栏高度（dp）。
 *
 * 优先用 `statusBarsIgnoringVisibility`：全屏进出、状态栏隐藏时数值不会瞬间变 0。
 * 刚进 Activity 的头一帧 window insets 可能还没下发（读到 0），这时用系统资源
 * `android:dimen/status_bar_height` 兜底——它是每台设备自己的真实高度，不是写死的 24dp。
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun rememberStatusBarTop(): Dp {
    val fromInsets = WindowInsets.statusBarsIgnoringVisibility
        .asPaddingValues()
        .calculateTopPadding()
    if (fromInsets > 0.dp) return fromInsets
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(context, density) {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) {
            with(density) { context.resources.getDimensionPixelSize(id).toDp() }
        } else {
            0.dp
        }
    }
}

/** 导航栏高度（dp）：Dialog 里 `navigationBarsPadding()` 偶尔读不到 insets，用系统资源兜底。 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun rememberNavigationBarBottom(): Dp {
    val fromInsets = WindowInsets.navigationBarsIgnoringVisibility
        .asPaddingValues()
        .calculateBottomPadding()
    if (fromInsets > 0.dp) return fromInsets
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(context, density) {
        val id = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id > 0) {
            with(density) { context.resources.getDimensionPixelSize(id).toDp() }
        } else {
            0.dp
        }
    }
}
