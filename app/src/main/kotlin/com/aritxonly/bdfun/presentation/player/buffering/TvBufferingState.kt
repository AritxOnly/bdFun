package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

internal class TvBufferingPresentationStateHolder {
    var dot1 by mutableFloatStateOf(0f)
    var dot2 by mutableFloatStateOf(0f)
    var dot3 by mutableFloatStateOf(0f)
}
@Composable
internal fun BindTvBufferingDots(ui: TvBufferingPresentationStateHolder) {
    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (startNanos == 0L) startNanos = now
                val t = ((now - startNanos) / 1_000_000f) % 2700f

                ui.dot1 = bufferingDot1(t)
                ui.dot2 = bufferingDot2(t)
                ui.dot3 = bufferingDot3(t)
            }
        }
    }

}
