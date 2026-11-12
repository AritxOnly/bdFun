package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.aritxonly.teak.common.settings.TeakSettingsActionRow
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsDropdownRow
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow
import com.aritxonly.bdfun.core.model.settings.ScrollTopPages

@Composable
internal fun SettingsAppearanceCard(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
    val ui = remember { SettingsAppearancePresentationStateHolder() }
    LaunchedEffect(settings.darkMode) { ui.pendingDark = null }
    LaunchedEffect(settings.colorStyle) { ui.pendingColor = null }
    TeakSettingsCard(title = "外观与显示") {
        SettingsThemeRows(runtime, ui)
        SettingsDisplayRows(runtime, ui)
    }
}

@Composable
private fun SettingsThemeRows(runtime: SettingsPageRuntime, ui: SettingsAppearancePresentationStateHolder) {
    val settings = runtime.settings
    val scope = runtime.scope
    TeakSettingsDropdownRow(
        title = "主题",
        options = listOf("跟随系统", "浅色", "深色"),
        selected = ui.pendingDark ?: settings.darkMode,
        onSelected = { index ->
            ui.pendingDark = index
            scope.launch {
                delay(260)
                settings.darkMode = index
            }
        },
    )
    TeakSettingsDropdownRow(
        title = "色彩风格",
        options = listOf("AC 粉", "Hyper 蓝"),
        selected = ui.pendingColor ?: settings.colorStyle,
        onSelected = { index ->
            ui.pendingColor = index
            scope.launch {
                delay(260)
                settings.colorStyle = index
                com.aritxonly.bdfun.ThemePalette.style = index
            }
        },
    )
    TeakSettingsDropdownRow(
        title = "界面风格",
        options = listOf("官方", "经典"),
        selected = settings.interfaceStyle.coerceIn(0, 1),
        onSelected = {
            settings.interfaceStyle = it
            com.aritxonly.teak.common.theme.TeakUiStyle.style = it
        },
    )
    TeakSettingsSwitchRow(
        title = "模糊效果",
        checked = settings.blurEnabled,
        onCheckedChange = { settings.blurEnabled = it },
    )
    TeakSettingsSwitchRow(
        title = "回到顶部控件",
        checked = settings.scrollTopEnabled,
        onCheckedChange = { settings.scrollTopEnabled = it },
    )
}

@Composable
private fun SettingsDisplayRows(runtime: SettingsPageRuntime, ui: SettingsAppearancePresentationStateHolder) {
    val settings = runtime.settings
    if (settings.scrollTopEnabled) {
        TeakSettingsDropdownRow(
            title = "回到顶部控件位置",
            options = listOf("左侧", "右侧"),
            selected = settings.scrollTopSide,
            onSelected = { settings.scrollTopSide = it },
        )
        // 每个页面单独开关；默认全部打开。
        ScrollTopPages.entries.forEach { (key, label) ->
            TeakSettingsSwitchRow(
                title = label,
                checked = settings.isScrollTopPageEnabled(key),
                onCheckedChange = { settings.setScrollTopPageEnabled(key, it) },
            )
        }
    }
    TeakSettingsSwitchRow(
        title = "减弱动态效果",
        checked = !settings.animationsEnabled,
        onCheckedChange = { settings.animationsEnabled = !it },
    )
    TeakSettingsSwitchRow(
        title = "智能刷新率",
        description = "根据画面状态自动调整屏幕刷新率",
        checked = settings.smartRefresh,
        onCheckedChange = { settings.smartRefresh = it },
    )
    TeakSettingsSwitchRow(
        title = "抬高底栏",
        description = "解决未开启安卓导航条时底栏位置过低的问题",
        checked = settings.bottomBarRaise,
        onCheckedChange = { settings.bottomBarRaise = it },
    )
    TeakSettingsActionRow(
        title = "设置开屏壁纸",
        trailing = if (settings.splashWallpaper.isBlank()) "未设置" else "已设置",
    ) {
        runtime.onPickWallpaper()
    }
    TeakSettingsActionRow(title = "清除开屏壁纸") {
        settings.splashWallpaper = ""
        runtime.navigation.onNotify("已清除开屏壁纸")
    }
    GlobalFontScaleRow(
        value = settings.fontScale,
        onValueChange = { settings.fontScale = it },
    )
}
