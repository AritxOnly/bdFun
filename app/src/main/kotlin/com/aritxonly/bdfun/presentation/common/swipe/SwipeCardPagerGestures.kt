package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.swipeCardPagerGestures(runtime: SwipeCardPagerRuntime): Modifier = with(runtime) {
    this@swipeCardPagerGestures
        .pointerInput(pageCount, widthPx) {
            var lastDragAt = 0L
            var dragVelocity = 0f // 页面/秒，向右为正
            detectHorizontalDragGestures(
                onDragStart = {
                    springJob?.cancel()
                    animating = false
                    velocity = 0f
                    dragVelocity = 0f
                    lastDragAt = 0L
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    val now = change.uptimeMillis
                    if (lastDragAt > 0L) {
                        val dt = (now - lastDragAt).coerceIn(8L, 64L) / 1000f
                        val instant = -dragAmount / widthPx.coerceAtLeast(1f) / dt
                        dragVelocity = dragVelocity * 0.7f + instant * 0.3f
                    }
                    lastDragAt = now
                    val delta = dragAmount / widthPx.coerceAtLeast(1f)
                    val next = progress - delta
                    progress = swipePagerResistance(next, pageCount)
                },
                onDragEnd = {
                    target = swipePagerTarget(progress, dragVelocity, selected, pageCount)
                    velocity = dragVelocity
                    startSpring()
                },
                onDragCancel = {
                    target = selected
                    velocity = 0f
                    startSpring()
                },
            )
        }
}
