package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.feature.dynamic.application.dynamicImageTransform

internal class DynamicImageViewerStateHolder { var shown by mutableStateOf(false) }
internal class DynamicImageZoomStateHolder {
    var scale by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
    fun transform(pressedCount: Int, zoomChange: Float, panChange: Offset): Boolean {
        val next = dynamicImageTransform(scale, pan.x, pan.y, pressedCount, zoomChange, panChange.x, panChange.y)
        scale = next.scale
        pan = Offset(next.panX, next.panY)
        return next.consume
    }
}
internal class DynamicImageBoundsStateHolder { val bounds = mutableStateMapOf<String, Rect>() }
