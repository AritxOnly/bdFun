package com.aritxonly.teak.common.modal

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.Scaffold

/** 全局弹层位于页面录制层之外；嵌套主题复用同一宿主。 */
@Composable
fun TeakModalHost(content: @Composable () -> Unit) {
    if (LocalAdvancedMaterialDialogBlurHostState.current != null) {
        content()
        return
    }
    ProvideAdvancedMaterialDialogBlurHost {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
            content()
        }
    }
}
