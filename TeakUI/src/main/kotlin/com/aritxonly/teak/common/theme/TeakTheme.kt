package com.aritxonly.teak.common.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakMaterialFineTuning
import com.aritxonly.teak.common.material.TeakMaterialSpec
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * TeakUI 的主题适配层。
 *
 * TeakUI 本身不绑定任何 App 的品牌色：宿主 App 传入 Miuix 的浅色/深色色板、
 * Material3 色板和字号倍率，这里统一注入 MiuixTheme、MaterialTheme、
 * LocalTeakMaterialSpec 和全局字号。
 */
@Composable
fun TeakTheme(
    dark: Boolean = isSystemInDarkTheme(),
    lightColors: Colors,
    darkColors: Colors,
    materialColors: ColorScheme,
    blurEnabled: Boolean = true,
    refractionEnabled: Boolean = true,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val mode = if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light
    val controller = remember(mode, lightColors, darkColors) {
        ThemeController(
            colorSchemeMode = mode,
            lightColors = lightColors,
            darkColors = darkColors,
        )
    }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * fontScale,
        )
    }
    val materialSpec = remember(blurEnabled, refractionEnabled) {
        TeakMaterialSpec(
            enabled = blurEnabled,
            blurEnabled = blurEnabled && Build.VERSION.SDK_INT >= 33,
            navigationTintAlpha = 0.72f,
            fineTuning = TeakMaterialFineTuning.Default.copy(
                refractionEnabled = refractionEnabled,
                postRefractionBlurEnabled = refractionEnabled,
            ),
        )
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MiuixTheme(controller = controller) {
            MaterialTheme(colorScheme = materialColors) {
                CompositionLocalProvider(LocalTeakMaterialSpec provides materialSpec) {
                    content()
                }
            }
        }
    }
}
