package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
internal fun DanmakuPanel(
    info: FullscreenPlayerInfo,
    onClose: () -> Unit,
) {

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            DanmakuSettingsContent(
                fullscreen = true,
                onInteract = {},
                danmakuItems = info.danmakuItems,
            )
        }
    }
}

internal fun Modifier.pointerInputSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onPressed: (Boolean) -> Unit,
    onChange: (Float) -> Unit,
): Modifier = this.pointerInput(range) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var decided = false
        var slider = false
        var lastX = down.position.x
        fun update(x: Float) {
            val ratio = (x / size.width).coerceIn(0f, 1f)
            onChange(range.start + ratio * (range.endInclusive - range.start))
        }
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val dx = change.position.x - down.position.x
            val dy = change.position.y - down.position.y
            if (!decided &&
                (kotlin.math.abs(dx) > viewConfiguration.touchSlop ||
                    kotlin.math.abs(dy) > viewConfiguration.touchSlop)
            ) {
                decided = true
                slider = kotlin.math.abs(dx) > kotlin.math.abs(dy)
                if (slider) {
                    onPressed(true)
                    update(change.position.x)
                }
            }
            if (slider) {
                lastX = change.position.x
                update(lastX)
                change.consume()
            }
        }
        if (slider) onPressed(false)
    }
}
