package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
internal fun DiscLifecycleEffects(runtime: DiscRuntime) {
    androidx.compose.runtime.SideEffect {
        if (runtime.ui.exiting && !runtime.ui.exitComposeLogged) {
            runtime.ui.exitComposeLogged = true
            runtime.dependencies.diagnostics.log("recomposed with exiting=true")
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        runtime.dependencies.diagnostics.onCreate()
        onDispose {
            runtime.dependencies.diagnostics.onDispose()
            runtime.dependencies.diagnostics.log("composable disposed")
        }
    }
}

@Composable
internal fun DiscExitEffect(runtime: DiscRuntime) {
    LaunchedEffect(runtime.ui.exiting) {
        if (!runtime.ui.exiting) return@LaunchedEffect
        runtime.dependencies.diagnostics.log("exit effect start, wait 280ms")
        delay(280)
        runtime.dependencies.diagnostics.log("calling BackgroundPlayback.exit()")
        runtime.dependencies.playback.exit()
        runtime.dependencies.diagnostics.log("BackgroundPlayback.exit() returned")
    }
}
