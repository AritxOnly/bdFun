package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Teak 的橙色。 */
internal val TeakOrange = Color(0xFFFF8A3D)

/** Bili 的主色：跟随 AC 粉 / Hyper 蓝。 */
internal val TeakBiliPink: Color
    get() = com.aritxonly.bdfun.BrandPink

/**
 * 开屏：Bili 先出现，Teak 后出现并带一点橙色辉光，收尾后交给调用方淡出。
 *
 * 总时长约 1.5s。刻意只用三段最便宜的动画（位移淡入 / 缩放落定 / 辉光），不引入着色器，
 * 保证低端机也是满帧，避免"开屏卡一下"。
 */
@Composable
fun TeakSplash(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    val biliAlpha = remember { Animatable(0f) }
    val biliShift = remember { Animatable(24f) }
    val biliScale = remember { Animatable(0.88f) }
    val teakAlpha = remember { Animatable(0f) }
    val teakShift = remember { Animatable(24f) }
    val teakScale = remember { Animatable(0.88f) }
    val glow = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.96f) }

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
                Spacer(Modifier.width(2.dp))
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
        }
    }
}

/** 一枚很淡的径向辉光，只负责"Teak 亮了一下"。 */
@Composable
private fun SplashGlow(progress: Float) {
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
