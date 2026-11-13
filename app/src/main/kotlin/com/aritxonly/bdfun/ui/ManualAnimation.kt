package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.delay

/**
 * 手动补间。本项目里基于 frame clock 的 `Animatable.animateTo` / `animateFloatAsState`
 * **不会推进**（已验证：target 更新了但值一直停在起点），所以统一用 `delay` 驱动的补间。
 */
@Composable
fun rememberManualProgress(
    target: Float,
    durationMs: Float = 160f,
    easing: androidx.compose.animation.core.Easing = FastOutSlowInEasing,
): Float {
    var value by remember { mutableFloatStateOf(target) }
    LaunchedEffect(target) {
        val from = value
        if (from == target) return@LaunchedEffect
        if (!TeakMotion.enabled) {
            value = target
            return@LaunchedEffect
        }
        if (durationMs <= 0f) {
            value = target
            return@LaunchedEffect
        }
        val startNanos = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - startNanos) / 1_000_000f / durationMs).coerceIn(0f, 1f)
            value = from + (target - from) * easing.transform(p)
            if (p >= 1f) break
            delay(8)
        }
        value = target
    }
    return value
}

/** 常用时长：页面/颜色切换统一走这个，保证各处速率一致。 */
const val UiSwapDurationMs = 200f
