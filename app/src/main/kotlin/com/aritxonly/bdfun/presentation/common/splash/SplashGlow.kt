package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** 一枚很淡的径向辉光，只负责"Teak 亮了一下"。 */
@Composable
internal fun SplashGlow(progress: Float) {
    Box(
        modifier = Modifier
            .size(260.dp)
            .graphicsLayer {
                alpha = progress * 0.55f
                val s = 0.7f + 0.3f * progress
                scaleX = s
                scaleY = s
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        TeakOrange.copy(alpha = 0.32f),
                        TeakBiliPink.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                ),
                CircleShape,
            ),
    )
}
