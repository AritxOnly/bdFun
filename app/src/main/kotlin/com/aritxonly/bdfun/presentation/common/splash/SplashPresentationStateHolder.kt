package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable

internal class SplashPresentationStateHolder {
    val biliAlpha = Animatable(0f)
    val biliShift = Animatable(24f)
    val biliScale = Animatable(0.88f)
    val teakAlpha = Animatable(0f)
    val teakShift = Animatable(24f)
    val teakScale = Animatable(0.88f)
    val glow = Animatable(0f)
    val logoScale = Animatable(0.96f)
}
