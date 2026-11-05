package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 「卡片互相推走」的横向切换：手指拖到哪，相邻卡片就跟到哪；
 * 松手后用弹簧继续滑到目标页（非线性、不淡入淡出）。
 *
 * 和首页底栏的 HorizontalPager 观感一致，但高度由内容自己决定，
 * 所以可以放在 LazyColumn 的单个 item 里（消息卡片、二级页列表卡片）。
 */
@Composable
fun SwipeCardPager(
    selected: Int,
    pageCount: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** 两张卡片之间留出的距离，不贴在一起。 */
    gap: Dp = 12.dp,
    /** 外部可以传同一个状态给 Tab 栏，让指示器跟着手指一起走。 */
    progressState: MutableFloatState? = null,
    content: @Composable (Int) -> Unit,
) {
    if (pageCount <= 0) return
    val scope = rememberCoroutineScope()
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    val sharedProgress = progressState ?: remember { mutableFloatStateOf(selected.toFloat()) }
    var progress by sharedProgress
    var target by remember { mutableIntStateOf(selected) }
    var velocity by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var springJob by remember { mutableStateOf<Job?>(null) }
    var animating by remember { mutableStateOf(false) }

    fun startSpring() {
        springJob?.cancel()
        springJob = scope.launch {
            animating = true
            var last = System.nanoTime()
            while (true) {
                val now = System.nanoTime()
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                last = now
                val accel = -400f * (progress - target) - 30f * velocity
                velocity += accel * dt
                progress += velocity * dt
                if (abs(progress - target) < 0.0015f && abs(velocity) < 0.02f) {
                    progress = target.toFloat()
                    velocity = 0f
                    break
                }
                kotlinx.coroutines.delay(8)
            }
            animating = false
            if (target != selected) onSelected(target)
        }
    }

    // 外部（点 Tab / 返回时）改 selected：同样用弹簧滑过去，而不是瞬移。
    LaunchedEffect(selected) {
        if (selected != target || abs(progress - selected) > 0.001f) {
            target = selected
            startSpring()
        }
    }

    // 首次组合时让共享进度和当前页对齐，避免重建后指示器停在 0。
    LaunchedEffect(Unit) {
        if (abs(progress - selected) > 0.001f) {
            progress = selected.toFloat()
            target = selected
        }
    }

    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(pageCount, widthPx) {
                var lastDragAt = 0L
                var dragVelocity = 0f // 页面/秒，向右为正
                detectHorizontalDragGestures(
                    onDragStart = {
                        springJob?.cancel()
                        animating = false
                        velocity = 0f
                        dragVelocity = 0f
                        lastDragAt = 0L
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val now = change.uptimeMillis
                        if (lastDragAt > 0L) {
                            val dt = (now - lastDragAt).coerceIn(8L, 64L) / 1000f
                            val instant = -dragAmount / widthPx.coerceAtLeast(1f) / dt
                            dragVelocity = dragVelocity * 0.7f + instant * 0.3f
                        }
                        lastDragAt = now
                        val delta = dragAmount / widthPx.coerceAtLeast(1f)
                        val next = progress - delta
                        progress = when {
                            next < 0f -> next * 0.32f
                            next > (pageCount - 1).toFloat() ->
                                (pageCount - 1) + (next - (pageCount - 1)) * 0.32f
                            else -> next
                        }
                    },
                    onDragEnd = {
                        val predicted = progress + dragVelocity * 0.16f
                        target = predicted.roundToInt().coerceIn(0, pageCount - 1)
                        // 位移很小、且几乎没有速度时回弹到原页，避免误触。
                        if (abs(predicted - selected) < 0.12f && abs(dragVelocity) < 0.25f) {
                            target = selected
                        }
                        velocity = dragVelocity
                        startSpring()
                    },
                    onDragCancel = {
                        target = selected
                        velocity = 0f
                        startSpring()
                    },
                )
            },
    ) { constraints ->
        val width = constraints.maxWidth.coerceAtLeast(1)
        val pages = (0 until pageCount).map { index ->
            subcompose(index) {
                Box(modifier = Modifier.fillMaxWidth()) { content(index) }
            }.map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
        }
        val heights = pages.map { items -> items.maxOfOrNull { it.height } ?: 0 }
        val lower = floor(progress).toInt().coerceIn(0, pageCount - 1)
        val upper = ceil(progress).toInt().coerceIn(0, pageCount - 1)
        // 滑动过程中取两页较高的高度，避免把正在进入的卡片裁剪掉；
        // 停稳后自然回到目标页自己的高度。
        val layoutHeight = maxOf(heights[lower], heights[upper])
        layout(width, layoutHeight) {
            val step = width + gapPx
            pages.forEachIndexed { index, placeables ->
                val x = ((index - progress) * step).roundToInt()
                placeables.forEach { placeable -> placeable.place(x, 0) }
            }
        }
    }
}
