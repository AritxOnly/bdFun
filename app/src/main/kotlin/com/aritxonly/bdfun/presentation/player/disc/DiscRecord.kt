package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.aritxonly.teak.common.material.TeakGlassSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DiscRecord(runtime: DiscRuntime, visual: DiscVisual) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 静态玻璃：不随唱片旋转，只画一次。
        // 模糊只给唱片本体，玻璃高光和外围光圈始终清晰。
        TeakGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
        ) {}

        // 旋转层：纹路、标记、封面一起转；它不包含玻璃，转起来不会触发背景采样。
        DiscSpinningFace(runtime, visual)

        // 中心小孔；对称，不需要转。
        Box(
            modifier = Modifier
                .size(runtime.size * 0.055f)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(colors.background.copy(alpha = 0.55f)),
        )
    }
}
