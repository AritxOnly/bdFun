package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SplashTeakWord(ui: SplashPresentationStateHolder) {
    val biliAlpha = ui.biliAlpha
    val biliShift = ui.biliShift
    val biliScale = ui.biliScale
    val teakAlpha = ui.teakAlpha
    val teakShift = ui.teakShift
    val teakScale = ui.teakScale
    val glow = ui.glow
    val logoScale = ui.logoScale
    Box {
        // 一层模糊的同色文本当作"发光"
        Text(
            text = "Teak",
            color = TeakOrange.copy(alpha = 0.85f),
            fontSize = 46.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1.5).sp,
            modifier = Modifier
                .graphicsLayer {
                    alpha = glow.value * teakAlpha.value
                    translationY = teakShift.value
                    scaleX = 1.06f
                    scaleY = 1.06f
                }
                .blur(14.dp),
        )
        Text(
            text = "Teak",
            color = TeakOrange,
            fontSize = 46.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1.5).sp,
            modifier = Modifier.graphicsLayer {
                alpha = teakAlpha.value
                translationY = teakShift.value
                val s = teakScale.value
                scaleX = s
                scaleY = s
            },
        )
    }
}

@Composable
internal fun SplashBiliWord(ui: SplashPresentationStateHolder) {
    val biliAlpha = ui.biliAlpha
    val biliShift = ui.biliShift
    val biliScale = ui.biliScale
    val teakAlpha = ui.teakAlpha
    val teakShift = ui.teakShift
    val teakScale = ui.teakScale
    val glow = ui.glow
    val logoScale = ui.logoScale
    Text(
        text = "Bili",
        color = TeakBiliPink,
        fontSize = 46.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.5).sp,
        modifier = Modifier.graphicsLayer {
            alpha = biliAlpha.value
            translationY = biliShift.value
            val s = biliScale.value
            scaleX = s
            scaleY = s
        },
    )
}
