package com.aritxonly.teak.common.chrome

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * 手动补间：不依赖 frame clock 的 `Animatable`，用 `delay` 逐帧推进。
 *
 * 目标值变化时从**当前值**重新起跑，因此中途反转也是连贯的。
 */
@Composable
fun rememberManualProgress(
    target: Float,
    durationMs: Float = 160f,
    easing: Easing = FastOutSlowInEasing,
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

/**
 * 真弹簧：用阻尼谐振子逐帧积分，有真实的过冲与回弹。
 * target 变化时从当前值和当前速度继续，所以中途打断也连贯。
 */
@Composable
fun rememberSpringFloat(
    target: Float,
    stiffness: Float = 320f,
    damping: Float = 22f,
    mass: Float = 1f,
): Float {
    var value by remember { mutableFloatStateOf(target) }
    var velocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) {
        if (!TeakMotion.enabled) {
            value = target
            velocity = 0f
            return@LaunchedEffect
        }
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            last = now
            val accel = (-stiffness * (value - target) - damping * velocity) / mass
            velocity += accel * dt
            value += velocity * dt
            if (kotlin.math.abs(value - target) < 0.0006f &&
                kotlin.math.abs(velocity) < 0.006f
            ) {
                value = target
                velocity = 0f
                break
            }
            delay(8)
        }
    }
    return value
}
