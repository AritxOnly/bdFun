package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DiscSurface(runtime: DiscRuntime, visual: DiscVisual, modifier: Modifier) {
    Box(modifier = modifier.size(runtime.size + 16.dp), contentAlignment = Alignment.Center) {
        if (visual.breathEnabled) DiscBreathGlow(visual)
        Box(modifier = discSurfaceModifier(runtime, visual), contentAlignment = Alignment.Center) {
            DiscRecord(runtime, visual)
        }
    }
}

@Composable
private fun discSurfaceModifier(runtime: DiscRuntime, visual: DiscVisual): Modifier {
    val colors = MiuixTheme.colorScheme
    val darkTheme = colors.background.luminance() < 0.5f
    return Modifier
        .size(runtime.size)
        .then(runtime.oneTake.modifier)
        .graphicsLayer {
            translationX = visual.dragX
            translationY = visual.dragY - visual.exitProgress * 26.dp.toPx()
            alpha = 1f - visual.exitProgress
            scaleX = if (runtime.ui.pressed) 0.96f else 1f
            scaleY = if (runtime.ui.pressed) 0.96f else 1f
        }
        .dropShadow(
            shape = CircleShape,
            shadow = Shadow(
                radius = 18.dp,
                spread = 0.dp,
                offset = DpOffset(0.dp, 5.dp),
                color = Color.Black,
                alpha = if (darkTheme) 0.36f else 0.20f,
            ),
        )
        .discPointerInput(runtime)
}

@Composable
private fun DiscBreathGlow(visual: DiscVisual) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - visual.exitProgress },
    ) {
        // 光圈比唱片外壳更大，允许画到 70dp 盒子之外，浅色背景下也能看出光晕在呼吸。
        val glowRadius = this.size.minDimension * (0.58f + 0.10f * visual.breath01)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    visual.breathTint.copy(alpha = 0.30f * visual.breath01),
                    visual.breathTint.copy(alpha = 0.72f * (0.30f + 0.70f * visual.breath01)),
                    visual.breathTint.copy(alpha = 0.20f * visual.breath01),
                    Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )
    }
}
