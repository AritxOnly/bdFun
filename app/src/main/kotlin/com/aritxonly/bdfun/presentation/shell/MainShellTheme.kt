package com.aritxonly.bdfun

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.findHostActivity
import com.aritxonly.bdfun.ui.syncSystemBarIcons

internal data class MainShellTheme(
    val dark: Boolean, val light: top.yukonga.miuix.kmp.theme.Colors,
    val darkColors: top.yukonga.miuix.kmp.theme.Colors, val materialColors: androidx.compose.material3.ColorScheme,
)

@Composable
internal fun rememberMainShellTheme(settings: SettingsBindings): MainShellTheme {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val light = remember(settings.colorStyle) { hyperLightColors() }
    val darkColors = remember(settings.colorStyle) { hyperDarkColors() }
    val materialColors = appMaterialColors(dark)

    val view = LocalView.current
    SideEffect {
        // 走 ContextWrapper 链找 Activity：直接强转在部分机型上会拿不到，图标就不跟着主题变
        val activity = view.context.findHostActivity()
        activity ?: return@SideEffect
        activity.syncSystemBarIcons(dark)
    }

    return MainShellTheme(dark, light, darkColors, materialColors)
}
