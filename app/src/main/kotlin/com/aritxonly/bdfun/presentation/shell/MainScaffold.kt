package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TeakChromeDefaults

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.ScrollTopButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
internal fun MainScaffold(settings: SettingsBindings, shell: MainShellState, dark: Boolean, background: Color) {
    val ui = shell.ui
    val session = shell.feeds.session
    val homeFeed = shell.feeds.home
    val dynamicFeed = shell.feeds.dynamic
    val tabPagerState = shell.navigation.pager
    val backdrop = shell.backdrops.main
    val wideNavigation = shell.backdrops.wideNavigation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        // 底栏玻璃的采样源：这一层就是整个页面内容。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            // 三个 tab：横向相连的 pager（可拖拽，切换是同一套非线性动画）
            // 外面包一层更大的触摸阈值，手指轻抖不会误翻页。
            MainTabPager(tabPagerState, homeFeed, dynamicFeed, session, settings, ui.dynamicRefreshTick)

        }

        val pageFloat = (tabPagerState.currentPage + tabPagerState.currentPageOffsetFraction)
            .coerceAtLeast(0f)
        ScrollTopButton(
            settings,
            bottomObstruction = if (ui.showSearch) 0.dp else
                TeakFloatingTabBarDefaults.Height + when {
                    wideNavigation -> 12.dp
                    settings.bottomBarRaise -> 4.dp + TeakChromeDefaults.RaisedBottomBarExtra
                    else -> 4.dp
                },
        )

        MainSearchLayer(settings, shell)
        MainHomeChrome(settings, shell, pageFloat)
        MainDynamicChrome(shell, pageFloat)

        MainShellNavigation(settings, shell, dark)
        // 换页时返回键回到"纯箭头"状态（pageKey / pageEnter 在上面声明）

    }
}
