package com.aritxonly.bdfun


import androidx.compose.foundation.background
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/** HyperOS 色板：B 站主色 + Miuix 表面层级。 */
private val AcPink = Color(0xFFFB7299)
private val AcPinkDeep = Color(0xFFE45C82)
private val HyperBlue = Color(0xFF3482FF)
private val HyperBlueDeep = Color(0xFF1D63D8)

/** 色板不再复制一份全局style；读取本容器的设置，Compose观察同一个来源。 */
internal class AppThemePalette(private val currentStyle: () -> Int) {
    val brand: Color get() = if (currentStyle() == 1) HyperBlue else AcPink
    val brandDeep: Color get() = if (currentStyle() == 1) HyperBlueDeep else AcPinkDeep
}

internal val BrandPink: Color get() = AppGraph.themePalette?.brand ?: AcPink
internal val BrandPinkDeep: Color get() = AppGraph.themePalette?.brandDeep ?: AcPinkDeep

internal fun hyperLightColors() = miuixLightColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    primaryVariant = BrandPink,
    onPrimaryVariant = Color(0xFF5A2735),
    secondaryVariant = BrandPink,
    onSecondaryVariant = Color.White,
    // 参考截图：页面底色浅灰 #F3F3F3，圆角卡片纯白 #FFFFFF
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF16181D),
    onBackgroundVariant = Color(0xFF6B7280),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF7F7F7),
    onSurfaceContainer = Color(0xFF16181D),
    surfaceContainerHigh = Color(0xFFF0F0F0),
    onSurfaceContainerHigh = Color(0xFF16181D),
    surfaceContainerHighest = Color(0xFFE9E9E9),
    onSurfaceContainerHighest = Color(0xFF16181D),
    outline = Color(0xFFE6E6E6),
    dividerLine = Color(0x14000000),
)

internal fun hyperDarkColors() = miuixDarkColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    primaryVariant = BrandPinkDeep,
    onPrimaryVariant = Color(0xFFFFD9E3),
    secondaryVariant = BrandPinkDeep,
    onSecondaryVariant = Color.White,
    background = Color(0xFF0D0E11),
    onBackground = Color(0xFFF2F3F5),
    onBackgroundVariant = Color(0xFF9AA0AA),
    surface = Color(0xFF121317),
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF1B1D22),
    surfaceContainer = Color(0xFF1A1C21),
    onSurfaceContainer = Color(0xFFF2F3F5),
    surfaceContainerHigh = Color(0xFF24272E),
    onSurfaceContainerHigh = Color(0xFFF2F3F5),
    surfaceContainerHighest = Color(0xFF2D3038),
    onSurfaceContainerHighest = Color(0xFFF2F3F5),
    outline = Color(0xFF2B2E35),
    dividerLine = Color(0x1AFFFFFF),
)

internal fun appMaterialColors(dark: Boolean): androidx.compose.material3.ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = BrandPink,
            background = Color(0xFF0D0E11),
            surface = Color(0xFF121317),
            surfaceVariant = Color(0xFF1A1C21),
            onSurface = Color(0xFFF2F3F5),
            outline = Color(0xFF2B2E35),
        )
    } else {
        lightColorScheme(
            primary = BrandPink,
            background = Color(0xFFF3F3F3),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF7F7F7),
            onSurface = Color(0xFF16181D),
            outline = Color(0xFFE6E6E6),
        )
    }

}
