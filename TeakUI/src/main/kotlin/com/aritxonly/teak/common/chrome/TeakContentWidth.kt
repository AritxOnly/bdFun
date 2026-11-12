package com.aritxonly.teak.common.chrome

import kotlin.math.roundToInt

/** 沿用原宽度规则，滚动手势重构不改变居中内容的排版。 */
internal fun resolveTeakContentWidth(
    availableWidth: Int?, explicitMaxWidth: Int?, wideBreakpoint: Int, defaultMaxWidth: Int,
): Int = when {
    explicitMaxWidth != null -> minOf(availableWidth ?: Int.MAX_VALUE, explicitMaxWidth)
    availableWidth == null -> defaultMaxWidth
    availableWidth >= wideBreakpoint -> minOf((availableWidth * 0.7f).roundToInt(), defaultMaxWidth)
    else -> availableWidth
}
