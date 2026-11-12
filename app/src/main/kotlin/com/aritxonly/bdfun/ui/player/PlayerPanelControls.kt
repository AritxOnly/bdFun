package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.kyant.shapes.Capsule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun PanelToggle(
    label: String,
    checked: Boolean,
    onInteract: () -> Unit,
    onChange: (Boolean) -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                onInteract()
                onChange(!checked)
            }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Color(0xFFCFD2DA), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(Capsule())
                .background(
                    if (checked) BrandPink else Color.White.copy(alpha = 0.16f),
                ),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (checked) 18.dp else 2.dp, y = 2.dp)
                    .size(16.dp)
                    .clip(Capsule())
                    .background(Color.White),
            )
        }
    }
}

/** 与播放器进度条完全同款：只保留轨道 + 填充，不要任何指示器。 */
@Composable
internal fun PanelSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    trackColor: Color,
    onChangeStart: () -> Unit = {},
    onValueChange: (Float) -> Unit,
) {

    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val ratio = ((value - range.start) / span).coerceIn(0f, 1f)
    var pressed by remember { mutableStateOf(false) }
    val grow = rememberManualProgress(if (pressed) 1f else 0f, 150f)
    val thickness = (5f + 3f * grow).dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(range) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onChangeStart()
                    pressed = true
                    fun update(x: Float) {
                        val r = (x / size.width).coerceIn(0f, 1f)
                        onValueChange(range.start + r * span)
                    }
                    update(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        update(change.position.x)
                        pressed = true
                        change.consume()
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val w = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thickness)
                .clip(Capsule())
                .background(trackColor),
        )
        Box(
            modifier = Modifier
                .width(w * ratio)
                .height(thickness)
                .clip(Capsule())
                .background(BrandPink),
        )
    }
}
