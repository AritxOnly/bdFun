package com.aritxonly.teak.common.chrome

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.squircle.addSquircleRect

/**
 * 连续曲率圆角（squircle），与 HyperOS / Miuix 的轮廓一致，
 * 而不是普通圆弧圆角。
 */
class TeakContinuousCornerShape(private val radius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(
                androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
            )
        }
        val path = Path().apply {
            addSquircleRect(
                width = size.width,
                height = size.height,
                cornerRadius = with(density) { radius.toPx() },
            )
        }
        return Outline.Generic(path)
    }
}
