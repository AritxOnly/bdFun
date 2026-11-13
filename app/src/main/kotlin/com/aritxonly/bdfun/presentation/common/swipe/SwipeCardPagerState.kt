package com.aritxonly.bdfun.ui

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class SwipeCardPagerPresentationStateHolder(selected: Int) {
    var target by mutableIntStateOf(selected)
    var velocity by mutableFloatStateOf(0f)
    var widthPx by mutableFloatStateOf(1f)
    var springJob by mutableStateOf<Job?>(null)
    var animating by mutableStateOf(false)
}
internal class SwipeCardPagerFallbackStateHolder(selected: Int) { val progress = mutableFloatStateOf(selected.toFloat()) }
internal class SwipeCardPagerRuntime(val selected: Int, val pageCount: Int, val onSelected: (Int) -> Unit,
    val progressState: MutableFloatState, val ui: SwipeCardPagerPresentationStateHolder,
    val scope: kotlinx.coroutines.CoroutineScope) {
    var progress by progressState
    var target: Int get() = ui.target
        set(value) { ui.target = value }
    var velocity: Float get() = ui.velocity
        set(value) { ui.velocity = value }
    var widthPx: Float get() = ui.widthPx
        set(value) { ui.widthPx = value }
    var springJob: Job? get() = ui.springJob
        set(value) { ui.springJob = value }
    var animating: Boolean get() = ui.animating
        set(value) { ui.animating = value }
    fun startSpring() {
        springJob?.cancel()
        springJob = scope.launch {
            animating = true
            var last = System.nanoTime()
            while (true) {
                val now = System.nanoTime()
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                last = now
                val accel = swipePagerAcceleration(progress, target, velocity)
                velocity += accel * dt
                progress += velocity * dt
                if (swipePagerAtRest(progress, target, velocity)) {
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

}
