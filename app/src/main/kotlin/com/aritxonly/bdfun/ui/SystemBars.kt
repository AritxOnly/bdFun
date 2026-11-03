package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.core.view.WindowCompat

/** 从任意 Context 往上找 Activity：Compose 的 LocalView.context 有时是 ContextThemeWrapper。 */
fun Context.findHostActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * 统一同步状态栏/导航栏图标明暗。
 *
 * 页面底色是浅色 → 用深色图标（isAppearanceLight* = true），反之亦然。
 * 各页面都必须走这里，避免有的页面按系统深色判断、有的按 App 内主题判断，
 * 出现"深色 App + 系统浅色"时图标看不清的问题。
 */
fun Activity.syncSystemBarIcons(dark: Boolean) {
    runCatching {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
