package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import com.aritxonly.bdfun.feature.video.application.DiscReleaseAction
import com.aritxonly.bdfun.feature.video.application.discDragOffset
import com.aritxonly.bdfun.feature.video.application.discReleaseAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun Modifier.discPointerInput(runtime: DiscRuntime): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (runtime.ui.exiting) {
            consumeDiscExitPointers()
            return@awaitEachGesture
        }
        down.consume()
        runtime.ui.pressed = true
        runtime.ui.longPressActive = false
        runtime.ui.dragTarget = Offset.Zero
        val generation = ++runtime.ui.pressGen
        val session = DiscPointerSession(down.position)
        startDiscBoostTimer(runtime, session, generation)
        awaitDiscMovement(runtime, session, down.id)
        releaseDiscGesture(runtime, session.total)
    }
}

/** 退出时仍等待抬手，避免 awaitEachGesture 在已抬手时形成无挂起循环。 */
private suspend fun AwaitPointerEventScope.consumeDiscExitPointers() {
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}

private fun startDiscBoostTimer(runtime: DiscRuntime, session: DiscPointerSession, generation: Int) {
    runtime.scope.launch {
        delay(380)
        if (runtime.ui.pressGen == generation && !session.dragging && !runtime.ui.longPressActive) {
            runtime.ui.longPressActive = true
            runtime.dependencies.playback.boostStart()
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitDiscMovement(runtime: DiscRuntime, session: DiscPointerSession, id: PointerId) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == id } ?: break
        val delta = change.position - session.last
        session.last = change.position
        if (change.pressed) {
            session.total += delta
            if (!runtime.ui.longPressActive) {
                if (!session.dragging && session.total.getDistance() > runtime.geometry.touchSlop) {
                    session.dragging = true
                    runtime.ui.pressGen += 1
                }
                val drag = discDragOffset(session.total.x, session.total.y, session.armed,
                    runtime.geometry.swipeThreshold, runtime.geometry.maxDragPx)
                session.armed = drag.armed
                if (drag.armed) runtime.ui.dragTarget = Offset(drag.x, drag.y)
            }
            change.consume()
        } else break
    }
}

private fun releaseDiscGesture(runtime: DiscRuntime, total: Offset) {
    val ui = runtime.ui
    val playback = runtime.dependencies.playback
    ui.pressed = false
    ui.pressGen += 1
    runtime.dependencies.diagnostics.log("release total=(${total.x.toInt()},${total.y.toInt()}) " +
        "longPress=${ui.longPressActive} exiting=${ui.exiting}")
    when (discReleaseAction(total.x, total.y, total.getDistance(), ui.longPressActive,
        runtime.geometry.swipeThreshold, runtime.geometry.touchSlop)) {
        DiscReleaseAction.EndBoost -> { ui.longPressActive = false; playback.boostEnd() }
        DiscReleaseAction.Next -> playback.next()
        DiscReleaseAction.Previous -> playback.previous()
        DiscReleaseAction.Exit -> {
            ui.exiting = true
            runtime.dependencies.diagnostics.log("UP_SWIPE -> exiting=true")
        }
        DiscReleaseAction.TogglePlayback -> playback.togglePlayPause()
        DiscReleaseAction.Open -> runtime.openOriginal()
        DiscReleaseAction.None -> Unit
    }
    ui.dragTarget = Offset.Zero
}
