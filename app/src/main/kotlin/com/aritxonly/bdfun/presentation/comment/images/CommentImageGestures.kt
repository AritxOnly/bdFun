package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.commentImageGestures(
    ui: CommentImagePageStateHolder, viewer: CommentImageViewerStateHolder,
): Modifier = pointerInput(Unit) {
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
                if (pressedCount >= 2) {
                    // 双指缩放/拖动：始终由图片处理。
                    ui.scale = (ui.scale * zoomChange).coerceIn(1f, 4f)
                    ui.pan = if (ui.scale > 1f) ui.pan + panChange else
                        androidx.compose.ui.geometry.Offset.Zero
                    event.changes.forEach { it.consume() }
                } else if (ui.scale > 1.01f) {
                    // 放大后单指移动图片。
                    ui.pan += panChange
                    event.changes.forEach { it.consume() }
                } else {
                    // 原始比例：单指横向拖动不消费，交给外层 Pager 翻页。
                    ui.pan = androidx.compose.ui.geometry.Offset.Zero
                }
                if (event.changes.none { it.pressed }) break
            }
            // 没放大时点一下空白：关闭大图。
            if (maxPressed <= 1 && !moved && ui.scale <= 1.01f) {
                viewer.close()
            }
        }
    }
