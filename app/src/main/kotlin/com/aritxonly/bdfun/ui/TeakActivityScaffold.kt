package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.hyperDarkColors
import com.aritxonly.bdfun.hyperLightColors
import com.aritxonly.teak.common.chrome.TeakPageChrome
import com.aritxonly.teak.common.chrome.TeakPageScaffold
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.theme.TeakTheme

/**
 * 独立 Activity 的统一外壳：主题 + 玻璃页面 chrome（顶栏 / 返回键 / backdrop）。
 *
 * 页面 chrome 本体已抽到 `TeakUI` 的 [TeakPageScaffold]，
 * 这里只负责把 App 的主题、色板和字号注入进去。
 *
 * [backRevealed] 传 null 表示跟随页面滚动；视频页传 false（纯箭头）。
 */
@Composable
fun TeakActivityScaffold(
    settings: SettingsStore,
    onBack: () -> Unit,
    backRevealed: Boolean? = null,
    showBackButton: Boolean = true,
    /** 播放器页自己就是视频，不需要再叠一张后台播放唱片。 */
    showBackgroundDisc: Boolean = true,
    /** 页面底部有输入栏时，把唱片整体抬高，避免和输入栏重叠。 */
    discLift: androidx.compose.ui.unit.Dp = 0.dp,
    /** 个别页面覆盖唱片的右边距，用来对准底部表情按钮。 */
    discEndPadding: androidx.compose.ui.unit.Dp? = null,
    title: String? = null,
    topBarLeading: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    topBarCenter: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
    topBarActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    /** 采集层之外的覆盖层：这里的玻璃可以安全读取 backdrop（真模糊）。 */
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
    /** 顶栏之上的浮层，玻璃菜单用，避免被搜索按钮等顶栏按钮盖住。 */
    menuOverlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
    titleAlwaysVisible: Boolean = false,
    pageState: TeakPageScrollState? = null,
    /** 顶栏渐变模糊的显隐：0 = 完全透明，1 = 正常显示。 */
    barBlurFraction: Float = 1f,
    /** 顶栏三个图标的自定义颜色；null 时跟随主题。 */
    topBarGlyphColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val discContext = LocalContext.current
    TeakBareTheme(settings) { _, materialColors ->
        TeakPageScaffold(
            title = title,
            onBack = onBack,
            backRevealed = backRevealed,
            showBackButton = showBackButton,
            pageState = pageState ?: com.aritxonly.teak.common.chrome.rememberTeakPageScrollState(),
            backgroundColor = materialColors.background,
            contentModifier = Modifier
                .background(materialColors.background)
                .smartRefresh(settings.smartRefresh),
            topBarLeading = topBarLeading,
            topBarCenter = topBarCenter,
            topBarActions = topBarActions,
            barBlurFraction = barBlurFraction,
            topBarGlyphColor = topBarGlyphColor,
            overlay = {
                ScrollTopButton(settings)
                overlay()
                // 后台播放时，除了播放器页，其他任意页面也保留一张唱片。
                if (!com.aritxonly.teak.common.chrome.isWideFoldTopBar() && showBackgroundDisc &&
                    settings.showDiscAfterVideo &&
                    BackgroundPlayback.active
                ) {
                    // 和首页完全同一份位置计算。
                    val discPlacement = rememberDiscPlacement()
                    BackgroundPlayDisc(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = discEndPadding ?: discPlacement.endPadding)
                            .graphicsLayer {
                                // 外壳自带 8dp 透明留白，下移后视觉底边和首页一致。
                                translationY = discPlacement.bottomOffset.toPx() - discLift.toPx()
                            },
                        size = discPlacement.size,
                    ) { video ->
                        com.aritxonly.bdfun.TeakNavigator.openVideo(discContext, video)
                    }
                }
            },
            menuOverlay = menuOverlay,
            titleAlwaysVisible = titleAlwaysVisible,
            content = content,
        )
    }
}

/**
 * 只注入主题、不套页面 chrome 的外壳。
 * 开始页 / 引导页要整屏铺自己的背景和顶栏，用这个。
 */
@Composable
fun TeakBareTheme(
    settings: SettingsStore,
    content: @Composable (dark: Boolean, materialColors: androidx.compose.material3.ColorScheme) -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val lightColors = remember(settings.colorStyle) { hyperLightColors() }
    val darkColors = remember(settings.colorStyle) { hyperDarkColors() }
    val materialColors = if (dark) {
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
    androidx.compose.runtime.SideEffect {
        com.aritxonly.teak.common.theme.TeakUiStyle.style = settings.interfaceStyle
    }
    TeakTheme(
        dark = dark,
        lightColors = lightColors,
        darkColors = darkColors,
        materialColors = materialColors,
        blurEnabled = settings.blurEnabled,
        refractionEnabled = settings.refractionEnabled,
        fontScale = settings.fontScale,
    ) {
        content(dark, materialColors)
    }
}
