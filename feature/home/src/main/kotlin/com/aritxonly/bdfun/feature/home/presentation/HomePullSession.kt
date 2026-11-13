package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/** 下拉仍用 8ms 手动缓动，避免 snapTo 排队或 spring 越过零点造成抽动。 */
internal class HomePullSession(
    private val state: HomeFeedState,
    private val ui: HomePresentationStateHolder,
    private val density: androidx.compose.ui.unit.Density,
    val thresholdPx: Float,
    private val maxPullPx: Float,
) {
    private fun applyPullRaw(px: Float) {
        val next = px.coerceIn(0f, maxPullPx)
        ui.pullOffset = next
        state.pullOffsetDp = with(density) { next.toDp().value }
        state.pullFraction = (next / thresholdPx).coerceIn(0f, 1f)
    }

    suspend fun animatePullTo(
        target: Float,
        durationMs: Float = 220f,
        easing: androidx.compose.animation.core.Easing =
            androidx.compose.animation.core.FastOutSlowInEasing,
    ) {
        val from = ui.pullOffset
        val to = target.coerceIn(0f, maxPullPx)
        if (from == to) return
        if (durationMs <= 0f) {
            applyPullRaw(to)
            return
        }
        val startNanos = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - startNanos) / 1_000_000f / durationMs)
                .coerceIn(0f, 1f)
            applyPullRaw(from + (to - from) * easing.transform(p))
            if (p >= 1f) break
            kotlinx.coroutines.delay(8)
        }
        applyPullRaw(to)
    }

    fun connection(): NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f && ui.pullOffset > 0f) {
                    val next = (ui.pullOffset + available.y).coerceAtLeast(0f)
                    val consumed = next - ui.pullOffset
                    applyPullRaw(next)
                    return Offset(0f, consumed)
                }
                if (available.y > 0f && !state.gridState.canScrollBackward) {
                    val next = (ui.pullOffset + available.y * 0.6f).coerceAtMost(maxPullPx)
                    val consumed = next - ui.pullOffset
                    applyPullRaw(next)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val shouldRefresh = ui.pullOffset >= thresholdPx &&
                    !state.refreshing && !state.loading
                if (shouldRefresh) {
                    animatePullTo(thresholdPx, 140f)
                    state.pullFraction = 1f
                    state.refresh()
                    // 回落交给 LaunchedEffect(state.refreshing)，这里保持阈值位置。
                } else {
                    animatePullTo(0f, 220f)
                    state.pullFraction = 0f
                }
                return Velocity.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
        }
}
