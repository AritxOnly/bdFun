package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TeakPageChrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.findHostActivity
import com.aritxonly.bdfun.ui.BackgroundPlayback

@Composable
internal fun MainShellEffects(settings: SettingsBindings, shell: MainShellState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ui = shell.ui
    val tabPagerState = shell.navigation.pager
    val selected = tabPagerState.currentPage
    // 首页在开屏下面先组合好（网络同时开跑），开屏淡出即交叉淡化进入首页，不白屏。
    // 系统返回键：先关搜索 / 覆盖页，再退出 App
    androidx.activity.compose.BackHandler(enabled = ui.showSearch) {
        if (ui.searchSubmitted != null) {
            ui.searchSubmitted = null
        } else {
            ui.closeSearch()
        }
    }
    // 后台播放时，从首页按返回不结束 App：整个任务收到后台，声音继续。
    androidx.activity.compose.BackHandler(
        enabled = !ui.showSearch &&
            settings.backgroundPlay &&
            BackgroundPlayback.active,
    ) {
        context.findHostActivity()?.moveTaskToBack(true)
    }

    // 当前页面标识：用于页面过渡动画 + 返回键状态复位
    val pageKey = if (ui.showSearch) "search" else "tab$selected"
    LaunchedEffect(pageKey) { TeakPageChrome.reset() }

}
