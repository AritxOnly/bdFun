package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TeakSplash(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val ui = remember { SplashPresentationStateHolder() }
    BindSplashEffects(ui, onFinished)
    SplashSurface(ui, modifier)
}
@Composable
private fun SplashSurface(ui: SplashPresentationStateHolder, modifier: Modifier) {
    val biliAlpha = ui.biliAlpha
    val biliShift = ui.biliShift
    val biliScale = ui.biliScale
    val teakAlpha = ui.teakAlpha
    val teakShift = ui.teakShift
    val teakScale = ui.teakScale
    val glow = ui.glow
    val logoScale = ui.logoScale
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            // 开屏期间吞掉点击，避免误触下面已经渲染好的首页
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            SplashGlow(progress = glow.value)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    val s = logoScale.value
                    scaleX = s
                    scaleY = s
                },
            ) {
                // "Teak" 后出现：先在原位留白，避免出现时把 Bili 挤动
                SplashTeakWord(ui)
                Spacer(Modifier.width(2.dp))
                SplashBiliWord(ui)
            }
        }
    }
}
