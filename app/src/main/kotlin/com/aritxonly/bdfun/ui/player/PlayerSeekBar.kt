package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue



/**
 * 进度条：小电视指示器（比上一版放大），按下/拖动恒为粗，抬手变细。
 */
@Composable
internal fun TeakSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    previewFrame: androidx.compose.ui.graphics.ImageBitmap? = null,
    previewTimeMs: Long = 0L,
    previewRatio: Float = 0f,
    onPreview: (ratio: Float, timeMs: Long) -> Unit = { _, _ -> },
    onPreviewEnd: () -> Unit = {},
) {

    val ui = remember { InlineSeekBarUiStateHolder() }
    val pressed by ui.pressed
    val dragRatio by ui.dragRatio
    val ratio = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shownRatio = if (dragRatio >= 0f) dragRatio else ratio
    val grow = rememberManualProgress(if (pressed) 1f else 0f, 150f)
    val thickness = (5f + 3f * grow).dp
    val thumbHeight = (20f + 4f * grow).dp

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .inlineSeekGestures(durationMs, ui, onSeek, onInteract, onPreview, onPreviewEnd),
        contentAlignment = Alignment.CenterStart,
    ) {
        val w = maxWidth
        InlineSeekPreview(w, previewFrame, previewTimeMs, previewRatio)
        InlineSeekTrack(w, shownRatio, thickness, thumbHeight)
    }
}


private fun Modifier.inlineSeekGestures(
    durationMs: Long, ui: InlineSeekBarUiStateHolder,
    onSeek: (Long) -> Unit, onInteract: () -> Unit,
    onPreview: (Float, Long) -> Unit, onPreviewEnd: () -> Unit,
): Modifier {
    var pressed by ui.pressed
    var dragRatio by ui.dragRatio
    return pointerInput(durationMs) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onInteract()
            if (durationMs > 0) {
                dragRatio = (down.position.x / size.width).coerceIn(0f, 1f)
                onPreview(dragRatio, (dragRatio * durationMs).toLong())
            }
            var released = false
            var lastInteract = System.currentTimeMillis()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    released = true
                    break
                }
                if (durationMs > 0) {
                    dragRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                    onPreview(dragRatio, (dragRatio * durationMs).toLong())
                }
                pressed = true
                val now = System.currentTimeMillis()
                if (now - lastInteract > 600) {
                    lastInteract = now
                    onInteract()
                }
                change.consume()
            }
            if (released && durationMs > 0 && dragRatio >= 0f) {
                onSeek((dragRatio * durationMs).toLong())
            }
            onPreviewEnd()
            dragRatio = -1f
            pressed = false
        }
}
}

@Composable
private fun InlineSeekPreview(
    w: androidx.compose.ui.unit.Dp, previewFrame: androidx.compose.ui.graphics.ImageBitmap?,
    previewTimeMs: Long, previewRatio: Float,
) {
    // 拖动时在进度条上方浮出画面预览小电视
    if (previewFrame != null) {
        val previewWidth = 148.dp
        val previewHeight = 84.dp
        val px = (w * previewRatio - previewWidth / 2f)
            .coerceIn(0.dp, (w - previewWidth).coerceAtLeast(0.dp))
        Box(
            modifier = Modifier
                .offset(x = px, y = -(previewHeight + 14.dp))
                .width(previewWidth)
                .height(previewHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
        ) {
            Image(
                bitmap = previewFrame,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = formatPlayTime(previewTimeMs),
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun InlineSeekTrack(
    w: androidx.compose.ui.unit.Dp, shownRatio: Float,
    thickness: androidx.compose.ui.unit.Dp, thumbHeight: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(Capsule())
            .background(Color(0x59FFFFFF)),
    )
    Box(
        modifier = Modifier
            .width(w * shownRatio)
            .height(thickness)
            .clip(Capsule())
            .background(Color.White),
    )
    val thumbWidth = thumbHeight * 0.94f
    val rawX = w * shownRatio - thumbWidth / 2f
    val maxX = (w - thumbWidth).coerceAtLeast(0.dp)
    val x = rawX.coerceIn(0.dp, maxX)
    Image(
        painter = painterResource(TeakIcon.SeekThumb),
        contentDescription = null,
        modifier = Modifier
            .padding(start = x)
            .size(width = thumbWidth, height = thumbHeight),
        contentScale = ContentScale.Fit,
    )
}
