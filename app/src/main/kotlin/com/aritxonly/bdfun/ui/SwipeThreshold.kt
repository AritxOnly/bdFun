package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize

/**
 * 给左右翻页加一点“起手门槛”。
 *
 * Compose 的 Pager 默认只要横向移动超过系统触摸阈值（约 8dp）就开始跟手，
 * 手指轻微一抖就会翻页。这里把阈值放大到 1.7 倍（约 13～14dp），
 * 其它参数全部沿用系统值；纵向滚动仍然用原来的判定逻辑。
 *
 * 只包在需要翻页的 Pager 外面，不影响视频手势、按钮点击和列表滚动。
 */
@Composable
fun rememberStricterSwipeConfiguration(scale: Float = 1.7f): ViewConfiguration {
    val base = LocalViewConfiguration.current
    return remember(base, scale) {
        object : ViewConfiguration {
            override val longPressTimeoutMillis: Long
                get() = base.longPressTimeoutMillis
            override val doubleTapTimeoutMillis: Long
                get() = base.doubleTapTimeoutMillis
            override val doubleTapMinTimeMillis: Long
                get() = base.doubleTapMinTimeMillis
            override val touchSlop: Float
                get() = base.touchSlop * scale
            override val minimumTouchTargetSize: DpSize
                get() = base.minimumTouchTargetSize
        }
    }
}
