package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal data class DynamicImageEntrance(
    val startScaleX: Float, val startScaleY: Float, val originCenter: Offset,
    val screenWidthPx: Float, val screenHeightPx: Float, val progress: Float, val alpha: Float,
)

@Composable
internal fun rememberDynamicImageEntrance(shown: Boolean, origin: Rect?): DynamicImageEntrance {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val startScaleX = (origin?.width?.div(screenWidthPx) ?: 0.9f).coerceIn(0.2f, 1f)
    val startScaleY = (origin?.height?.div(screenHeightPx) ?: 0.9f).coerceIn(0.2f, 1f)
    val originCenter = origin?.center ?: Offset(screenWidthPx / 2f, screenHeightPx / 2f)
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 220,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "dynamic-image-zoom",
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "dynamic-image-alpha",
    )
    return DynamicImageEntrance(startScaleX, startScaleY, originCenter, screenWidthPx, screenHeightPx, progress, alpha)
}
