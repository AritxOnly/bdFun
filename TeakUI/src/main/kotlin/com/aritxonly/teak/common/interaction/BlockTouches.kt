package com.aritxonly.teak.common.interaction

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** 透明但仍在屏幕上的控件会抢点击：用它把手势吃掉。 */
fun Modifier.blockTouches(blocked: Boolean): Modifier = pointerInput(blocked) {
    if (!blocked) return@pointerInput
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        }
    }
}
