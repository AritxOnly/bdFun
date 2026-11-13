package com.aritxonly.teak.common.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 平铺内容页使用绝对黑白底色，分组区域使用独立中性色，避免跟随卡片色阶。 */
data class TeakContentColors(val background: Color, val groupedBackground: Color)

internal fun teakContentColors(dark: Boolean) = if (dark) {
    TeakContentColors(Color.Black, Color(0xFF161616))
} else {
    TeakContentColors(Color.White, Color(0xFFF3F3F3))
}

val LocalTeakContentColors = staticCompositionLocalOf { teakContentColors(false) }
