package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToSpace
import com.aritxonly.teak.common.chrome.LocalTeakPageScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.SearchOverlay
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
internal fun MainSearchLayer(settings: SettingsBindings, shell: MainShellState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val ui = shell.ui
    val searchChrome = shell.chrome.search
    val searchBackdrop = shell.backdrops.search
    val searchReveal = shell.searchReveal
    // 搜索页：搜索框保持不动，下面整块换成纯白页（搜索记录 + 热搜 / 结果）
    if (ui.showSearch || searchReveal > 0.01f) {
        // 搜索页复刻首页结构：内容先录进 searchBackdrop，
        // 顶栏的渐变模糊和筛选菜单都采样同一块。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(searchBackdrop),
        ) {
            CompositionLocalProvider(
                LocalTeakBackdrop provides searchBackdrop,
                LocalTeakPageScrollState provides searchChrome,
            ) {
                SearchOverlay(
                    settings = settings,
                    submitted = ui.searchSubmitted,
                    reveal = searchReveal,
                    query = ui.searchQuery,
                    onPickKeyword = { word ->
                        ui.searchQuery = word
                        settings.addSearchHistory(word)
                        ui.searchSubmitted = word
                        keyboard?.hide()
                    },
                    onOpenVideo = { navigateToVideo(context, it) },
                    onOpenUser = { navigateToSpace(context, it.mid) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

}
