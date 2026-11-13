package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlin.math.abs

@Composable
internal fun BindSwipeCardPagerSelection(runtime: SwipeCardPagerRuntime) {
    with(runtime) {
        // 外部（点 Tab / 返回时）改 selected：同样用弹簧滑过去，而不是瞬移。
        LaunchedEffect(selected) {
            if (selected != target || abs(progress - selected) > 0.001f) {
                target = selected
                startSpring()
            }
        }

        // 首次组合时让共享进度和当前页对齐，避免重建后指示器停在 0。
        LaunchedEffect(Unit) {
            if (abs(progress - selected) > 0.001f) {
                progress = selected.toFloat()
                target = selected
            }
        }

    }
}
