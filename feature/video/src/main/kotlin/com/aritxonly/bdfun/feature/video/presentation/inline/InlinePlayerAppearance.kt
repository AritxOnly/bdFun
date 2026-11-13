package com.aritxonly.bdfun.ui

import androidx.compose.ui.graphics.Color

/** app提供原图标资源与强调色，模块不读取业务资源目录。 */
data class InlinePlayerAppearance(val playIcon: Int, val fullscreenIcon: Int, val accentColor: Color)
