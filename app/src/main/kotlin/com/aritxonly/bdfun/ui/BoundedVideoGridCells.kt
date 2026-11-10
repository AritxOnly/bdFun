package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/** Fill as many readable cards as fit, without stretching cards on larger windows. */
internal class BoundedVideoGridCells(private val minSize: Dp, private val maxSize: Dp) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> =
        boundedVideoCellSizes(availableSize, spacing, minSize.roundToPx(), maxSize.roundToPx())
}

internal fun boundedVideoCellSizes(available: Int, spacing: Int, min: Int, max: Int): List<Int> {
    require(min > 0 && max >= min && spacing >= 0)
    val width = available.coerceAtLeast(0)
    val count = ((width + spacing) / (min + spacing)).coerceAtLeast(1)
    val cardSpace = (width - spacing * (count - 1)).coerceAtLeast(0)
    val base = (cardSpace / count).coerceAtMost(max)
    val remainder = if (base < max) cardSpace % count else 0
    return List(count) { base + if (it < remainder) 1 else 0 }
}
