package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun Modifier.videoActionPressInput(
    state: VideoActionPressStateHolder, onClick: () -> Unit,
): Modifier = pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startX = down.position.x
        val startY = down.position.y
        // 所有按钮按住期间都保持压满状态
        state.isPressing = true
        state.press = 1f
        var canceled = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            // 父级翻页/滚动一旦接管，就不再算点击。
            if (change.isConsumed ||
                kotlin.math.abs(change.position.x - startX) > touchSlop ||
                kotlin.math.abs(change.position.y - startY) > touchSlop
            ) {
                canceled = true
                break
            }
        }
        state.isPressing = false
        if (!canceled && !state.longPressFired) onClick()
    }
}

@Composable
internal fun VideoActionVisual(
    modifier: Modifier, label: String, value: Long, kind: ActionKind,
    onClick: () -> Unit, hasLongPress: Boolean, state: VideoActionPressStateHolder, animatedTint: Color,
) {
    Column(
        modifier = modifier
            .width(58.dp)
            // 不要 clip：外扩的圆环需要画出 36dp 图标框之外
            .videoActionPressInput(state, onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (hasLongPress) {
                LikeRipple(
                    holding = state.rippleHolding,
                    burst = state.rippleBurst,
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        val s = 1f - 0.09f * state.press
                        scaleX = s
                        scaleY = s
                    }
                    .blur((2.8f * state.press).dp),
                contentAlignment = Alignment.Center,
            ) {
                ResourceGlyph(
                    res = kind.res,
                    tint = animatedTint,
                    size = 27.dp,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (value > 0) formatCount(value) else label,
            color = animatedTint,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
