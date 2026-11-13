package com.aritxonly.teak.common.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 平铺内容页使用绝对黑白底色，分组区域使用独立中性色，避免跟随卡片色阶。 */
data class TeakContentColors(val background: Color, val groupedBackground: Color,
    val controlFill: Color, val controlOutline: Color,
)

internal fun teakContentColors(dark: Boolean) = if (dark) {
    TeakContentColors(Color.Black, Color(0xFF161616), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.18f))
} else {
    TeakContentColors(Color.White, Color(0xFFF3F3F3), Color.Black.copy(alpha = 0.045f), Color.Black.copy(alpha = 0.12f))
}

val LocalTeakContentColors = staticCompositionLocalOf { teakContentColors(false) }

/** 仅在绝对黑白内容页提高玻璃控件的轮廓对比度。 */
val LocalTeakControlContrast = staticCompositionLocalOf { false }
