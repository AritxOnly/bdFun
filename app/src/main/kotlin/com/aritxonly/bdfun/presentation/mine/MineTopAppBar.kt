package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.AdaptiveMiuixTopAppBar
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassRecipes
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MineTopAppBar(onSettings: () -> Unit) {
    val material = LocalTeakMaterialSpec.current
    val recipe = remember(material) { TeakGlassRecipes.floatingNavigation(material.fineTuning) }
    AdaptiveMiuixTopAppBar(title = "", largeTitle = "", color = Color.Transparent, actions = {
        // 复用主壳采样和底栏同款柔光材质，不给单个按钮另建采样层。
        TeakGlassIconButton(onClick = onSettings, size = 40.dp, recipe = recipe,
            backdrop = LocalTeakBackdrop.current, materialAlpha = 1f) {
            Icon(MiuixIcons.Settings, "设置", tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp))
        }
    })
}
