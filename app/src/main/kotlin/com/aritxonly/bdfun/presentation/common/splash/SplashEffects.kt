package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun BindSplashEffects(ui: SplashPresentationStateHolder, onFinished: () -> Unit) {
    val biliAlpha = ui.biliAlpha
    val biliShift = ui.biliShift
    val biliScale = ui.biliScale
    val teakAlpha = ui.teakAlpha
    val teakShift = ui.teakShift
    val teakScale = ui.teakScale
    val glow = ui.glow
    val logoScale = ui.logoScale
    val settle = spring<Float>(dampingRatio = 0.62f, stiffness = 260f)

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, settle) }

        // 1) Bili 先来
        launch { biliAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        launch { biliShift.animateTo(0f, settle) }
        launch { biliScale.animateTo(1f, settle) }

        // 2) Teak 跟上，同时亮起辉光
        delay(200)
        launch { teakAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
        launch { teakShift.animateTo(0f, settle) }
        launch { teakScale.animateTo(1f, settle) }
        launch { glow.animateTo(1f, tween(480, easing = LinearOutSlowInEasing)) }

        delay(1100)
        onFinished()
    }

}
