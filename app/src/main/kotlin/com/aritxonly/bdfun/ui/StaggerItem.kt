package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 甩尾式入场：按 index 依次错开（每格 [stepMs]），配合淡入 + 轻微上移。
 * 用 delay 手动补间（本项目 frame clock 动画不推进）。
 */
@Composable
fun StaggerItem(
    index: Int,
    modifier: Modifier = Modifier,
    stepMs: Int = 32,
    durationMs: Int = 260,
    maxStagger: Int = 8,
    content: @Composable () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(index) {
        val delayMs = (index.coerceIn(0, maxStagger)) * stepMs
        delay(delayMs.toLong())
        val start = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - start) / 1_000_000f / durationMs).coerceIn(0f, 1f)
            progress = FastOutSlowInEasing.transform(p)
            if (p >= 1f) break
            delay(8)
        }
        progress = 1f
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 26.dp.toPx()
        },
    ) {
        content()
    }
}
