package com.aritxonly.teak.common.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.aritxonly.teak.common.material.teakGlassShadow

fun Modifier.teakFloatingNavigationShadow(
    shape: Shape,
    isDark: Boolean,
): Modifier {
    return teakGlassShadow(
        shape = shape,
        isDark = isDark,
    )
}
