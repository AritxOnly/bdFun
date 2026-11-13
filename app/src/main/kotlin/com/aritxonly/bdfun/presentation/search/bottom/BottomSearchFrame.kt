package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class BottomSearchTransition(
    val reveal: Float, val windowAlpha: Float, val maskAlpha: Float, val contentAlpha: Float,
)

@Composable
internal fun BottomSearchFrame(transition: BottomSearchTransition, content: @Composable () -> Unit) {
    val liftPx = with(LocalDensity.current) { TeakSearchTransitionDefaults.ContentLift.toPx() }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = transition.windowAlpha.coerceIn(0f, 1f) }
        .background(MiuixTheme.colorScheme.background.copy(alpha = transition.maskAlpha.coerceIn(0f, 1f)))) {
        Box(Modifier.matchParentSize().blockTouches(true))
        Box(Modifier.fillMaxSize().graphicsLayer {
            translationY = (1f - transition.reveal.coerceIn(0f, 1f)) * liftPx
            alpha = transition.contentAlpha.coerceIn(0f, 1f)
        }) { content() }
    }
}
