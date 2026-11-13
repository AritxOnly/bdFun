package com.aritxonly.bdfun.ui


internal fun bufferingDot1(t: Float): Float = when {
    t < 250f -> t / 250f
    t < 1400f -> 1f
    t < 1650f -> 1f - (t - 1400f) / 250f
    else -> 0f
}
internal fun bufferingDot2(t: Float): Float = when {
    t < 250f -> 0f
    t < 500f -> (t - 250f) / 250f
    t < 1650f -> 1f
    t < 1900f -> 1f - (t - 1650f) / 250f
    else -> 0f
}
internal fun bufferingDot3(t: Float): Float = when {
    t < 500f -> 0f
    t < 750f -> (t - 500f) / 250f
    t < 1900f -> 1f
    t < 2150f -> 1f - (t - 1900f) / 250f
    else -> 0f
}
