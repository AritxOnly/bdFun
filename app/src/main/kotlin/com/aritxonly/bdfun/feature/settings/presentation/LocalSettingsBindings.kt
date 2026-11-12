package com.aritxonly.bdfun.feature.settings.presentation

import androidx.compose.runtime.staticCompositionLocalOf

/** 由 Activity 的装配/主题层提供，不在控件内回查进程单例。 */
internal val LocalSettingsBindings = staticCompositionLocalOf<SettingsBindings> { error("设置投影尚未装配") }
