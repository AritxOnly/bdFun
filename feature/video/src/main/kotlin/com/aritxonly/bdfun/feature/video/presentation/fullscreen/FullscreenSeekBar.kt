package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun FullscreenSeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long = 0L,
    onInteract: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    externalPressed: Boolean = false,
    externalLook: Float = 0f,
    onScrubStart: () -> Unit = {},
    onScrubEnd: () -> Unit = {},
    onLookChanged: (Float) -> Unit = {},
    onScrubPreview: (Long) -> Unit = {},
) {

    val ui = remember { FullscreenSeekBarUiStateHolder() }
    val pressed by ui.pressed
    val dragRatio by ui.dragRatio
    val dragLook by ui.dragLook
    val ratio = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shown = if (dragRatio >= 0f) dragRatio else ratio
    BoxWithConstraints(
        modifier = modifier
            .height(24.dp)
            .fullscreenSeekGestures(durationMs, ui, onInteract, onSeek, onScrubStart, onScrubEnd, onLookChanged, onScrubPreview),
        contentAlignment = Alignment.CenterStart,
    ) {
        FullscreenSeekTrack(maxWidth, shown, durationMs, bufferedMs, pressed || externalPressed, dragLook, externalLook)
    }
}

private fun Modifier.fullscreenSeekGestures(
    durationMs: Long, ui: FullscreenSeekBarUiStateHolder,
    onInteract: () -> Unit, onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit, onScrubEnd: () -> Unit,
    onLookChanged: (Float) -> Unit, onScrubPreview: (Long) -> Unit,
): Modifier {
    var pressed by ui.pressed
    var dragRatio by ui.dragRatio
    var dragLook by ui.dragLook
    return pointerInput(durationMs) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            pressed = true
            dragLook = 0f
            onScrubStart()
            onLookChanged(0f)
            var lastX = down.position.x
            onInteract()
            if (durationMs > 0) {
                dragRatio = (down.position.x / size.width).coerceIn(0f, 1f)
                onScrubPreview((dragRatio * durationMs).toLong())
            }
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dx = change.position.x - lastX
                if (kotlin.math.abs(dx) > 0.5f) {
                    dragLook = (dragLook + dx / (size.width * 0.08f))
                        .coerceIn(-1f, 1f)
                    onLookChanged(dragLook)
                }
                lastX = change.position.x
                if (durationMs > 0) {
                    dragRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                    onScrubPreview((dragRatio * durationMs).toLong())
                }
                change.consume()
            }
            if (durationMs > 0 && dragRatio >= 0f) {
                onSeek((dragRatio * durationMs).toLong())
            }
            dragRatio = -1f
            dragLook = 0f
            pressed = false
            onScrubEnd()
            onLookChanged(0f)
            onScrubPreview(-1L)
        }
}
}

@Composable
private fun FullscreenSeekTrack(
    w: androidx.compose.ui.unit.Dp, shown: Float, durationMs: Long, bufferedMs: Long,
    thick: Boolean, dragLook: Float, externalLook: Float,
) {
    val thickness = if (thick) 4.dp else 2.5.dp
    val seekPink = Color(0xFFFB7299)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(Capsule())
            .background(Color.White.copy(alpha = 0.3f)),
    )
    val bufferedRatio = if (durationMs > 0) {
        (bufferedMs.toFloat() / durationMs).coerceIn(shown, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .width(w * bufferedRatio)
            .height(thickness)
            .clip(Capsule())
            .background(Color.White.copy(alpha = 0.52f)),
    )
    Box(
        modifier = Modifier
            .width(w * shown)
            .height(thickness)
            .clip(Capsule())
            .background(seekPink),
    )
    val thumbSize = 17.6.dp
    val activeLook = if (kotlin.math.abs(dragLook) > 0.01f) dragLook else externalLook
    val thumbX = (w * shown - thumbSize / 2f)
        .coerceIn(0.dp, (w - thumbSize).coerceAtLeast(0.dp))
    TvSeekThumb(
        look = activeLook,
        modifier = Modifier
            .padding(start = thumbX)
            .offset(y = (-1).dp)
            .size(thumbSize),
    )
}
