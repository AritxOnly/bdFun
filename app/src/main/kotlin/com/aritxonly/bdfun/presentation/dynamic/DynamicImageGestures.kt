package com.aritxonly.bdfun.ui

import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.dynamicImageGestures(ui: DynamicImageZoomStateHolder, dismissAnimated: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var moved = false
            var last = down.position
            var maxPressed = 1
            while (true) {
                val event = awaitPointerEvent()
                val pressedCount = event.changes.count { it.pressed }
                maxPressed = maxOf(maxPressed, pressedCount)
                val change = event.changes.firstOrNull()
                if (change != null) {
                    if ((change.position - last).getDistance() > 12f) moved = true
                    last = change.position
                }
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (ui.transform(pressedCount, zoomChange, panChange)) {
                    event.changes.forEach { it.consume() }
                }
                if (event.changes.none { it.pressed }) break
            }
            // 没放大时点一下空白：关闭。
            if (maxPressed <= 1 && !moved && ui.scale <= 1.01f) {
                dismissAnimated()
            }
        }
    }
