package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
internal fun DiscPlaybackEffects(runtime: DiscRuntime) {
    val playing = runtime.session.playing
    val breathEnabled = runtime.settings.discBreathEnabled && playing
    LaunchedEffect(breathEnabled) {
        if (!breathEnabled) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) {
            runtime.ui.breathPhase = ((System.nanoTime() - start) / 1_000_000f % 2400f) / 2400f
            delay(33)
        }
    }
    LaunchedEffect(playing, runtime.ui.exiting) {
        if (!playing || runtime.ui.exiting) return@LaunchedEffect
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = (now - last) / 1_000_000_000f
            last = now
            runtime.ui.spin = (runtime.ui.spin + dt * 55f) % 360f
            delay(33L)
        }
    }
}
