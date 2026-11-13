package com.aritxonly.bdfun.ui

import kotlin.math.abs
import kotlin.math.roundToInt

internal fun swipePagerAcceleration(progress: Float, target: Int, velocity: Float) =
    -400f * (progress - target) - 30f * velocity
internal fun swipePagerAtRest(progress: Float, target: Int, velocity: Float) =
    abs(progress - target) < 0.0015f && abs(velocity) < 0.02f
internal fun swipePagerResistance(next: Float, pageCount: Int): Float = when {
    next < 0f -> next * 0.32f
    next > (pageCount - 1).toFloat() -> (pageCount - 1) + (next - (pageCount - 1)) * 0.32f
    else -> next
}
internal fun swipePagerTarget(progress: Float, dragVelocity: Float, selected: Int, pageCount: Int): Int {
    val predicted = progress + dragVelocity * 0.16f
    var target = predicted.roundToInt().coerceIn(0, pageCount - 1)
    if (abs(predicted - selected) < 0.12f && abs(dragVelocity) < 0.25f) target = selected
    return target
}
