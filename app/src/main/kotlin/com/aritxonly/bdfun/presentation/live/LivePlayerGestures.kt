package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal data class LiveGestureCallbacks(
    val ignoreDown: (Offset, Float) -> Boolean,
    val onTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onVerticalDragStart: (leftSide: Boolean) -> Unit,
    val onVerticalDrag: (leftSide: Boolean, totalDy: Float) -> Unit,
    val onVerticalDragEnd: () -> Unit,
    val onCenterSwipe: () -> Unit,
)


@Composable
internal fun Modifier.livePlayerGestures(
    ignoreDown: (Offset, Float) -> Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onVerticalDragStart: (leftSide: Boolean) -> Unit,
    onVerticalDrag: (leftSide: Boolean, totalDy: Float) -> Unit,
    onVerticalDragEnd: () -> Unit,
    onCenterSwipe: () -> Unit,
): Modifier {
    val settings = LocalSettingsBindings.current
    val tapState = remember { longArrayOf(0L) }
    val pendingTap = remember { arrayOfNulls<Job>(1) }
    val scope = rememberCoroutineScope()
    val callbacks by rememberUpdatedState(
        LiveGestureCallbacks(
            ignoreDown = ignoreDown,
            onTap = onTap,
            onDoubleTap = onDoubleTap,
            onVerticalDragStart = onVerticalDragStart,
            onVerticalDrag = onVerticalDrag,
            onVerticalDragEnd = onVerticalDragEnd,
            onCenterSwipe = onCenterSwipe,
        ),
    )

    return livePointerInput(settings, { callbacks }, tapState, pendingTap, scope)
}

private fun Modifier.livePointerInput(settings: SettingsBindings, callbacks: () -> LiveGestureCallbacks,
    tapState: LongArray, pendingTap: Array<Job?>, scope: CoroutineScope): Modifier {
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (ignoreLiveDown(down.position, callbacks())) return@awaitEachGesture

            val side = when {
                down.position.x < size.width * 0.28f -> true
                down.position.x > size.width * 0.72f -> false
                else -> null
            }
            val verticalAllowed = settings.gestureBrightnessVolume &&
                side != null &&
                down.position.y > 80.dp.toPx() &&
                down.position.y < size.height - 100.dp.toPx()
            val centerSwipeEnabled = settings.gestureCenterFullscreen && side == null
            val leftSide = side ?: (down.position.x < size.width / 2f)

            var dragged = false
            var vertical = false
            var startedVertical = false
            var centerSwipe = false
            var centerSwipeDy = 0f
            var totalDy = 0f
            var totalDx = 0f
            var previous = down
            val downTime = System.currentTimeMillis()

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (!dragged) {
                        dispatchLiveTap(downTime, tapState, pendingTap, scope) { callbacks() }
                    }
                    break
                }
                val dx = change.position.x - previous.position.x
                val dy = change.position.y - previous.position.y
                totalDx += dx
                totalDy += dy
                previous = change
                if (!dragged &&
                    (abs(totalDy) > viewConfiguration.touchSlop ||
                        abs(totalDx) > viewConfiguration.touchSlop)
                ) {
                    dragged = true
                    val verticalDrag = abs(totalDy) >= abs(totalDx)
                    centerSwipe = verticalDrag && centerSwipeEnabled
                    vertical = verticalDrag && verticalAllowed && !centerSwipe
                    if (vertical) {
                        startedVertical = true
                        tapState[0] = 0
                        pendingTap[0]?.cancel()
                        pendingTap[0] = null
                        callbacks().onVerticalDragStart(leftSide)
                    } else if (centerSwipe) {
                        tapState[0] = 0
                        pendingTap[0]?.cancel()
                        pendingTap[0] = null
                    }
                }
                if (dragged && centerSwipe) {
                    centerSwipeDy = totalDy
                    change.consume()
                } else if (dragged && vertical) {
                    callbacks().onVerticalDrag(leftSide, totalDy)
                    change.consume()
                }
            }
            if (startedVertical) callbacks().onVerticalDragEnd()
            if (centerSwipe && abs(centerSwipeDy) > 70.dp.toPx()) callbacks().onCenterSwipe()
        }
    }
}


private fun dispatchLiveTap(downTime: Long, tapState: LongArray, pendingTap: Array<Job?>,
    scope: CoroutineScope, callbacks: () -> LiveGestureCallbacks) {
                        val held = System.currentTimeMillis() - downTime
                        if (held < 300) {
                            val now = System.currentTimeMillis()
                            if (now - tapState[0] < 260) {
                                tapState[0] = 0
                                pendingTap[0]?.cancel()
                                pendingTap[0] = null
                                callbacks().onDoubleTap()
                            } else {
                                tapState[0] = now
                                pendingTap[0]?.cancel()
                                pendingTap[0] = scope.launch {
                                    delay(260)
                                    if (tapState[0] != 0L) callbacks().onTap()
                                }
                            }
                        }
}

private suspend fun AwaitPointerEventScope.ignoreLiveDown(position: Offset, callbacks: LiveGestureCallbacks): Boolean {
    if (callbacks.ignoreDown(position, size.height.toFloat())) {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            if (event.changes.none { it.pressed }) break
        }
        return true
    }
    val systemEdge = 24.dp.toPx()
    if (position.x < systemEdge || position.x > size.width - systemEdge) {
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.none { it.pressed }) break
        }
        return true
    }
    return false
}
