package com.aritxonly.bdfun.core.application

/** 播放结束只在进入 Ended 时触发一次，确认后轮询不能再次触发自动连播。 */
fun reducePlaybackSnapshot(
    before: PlaybackSnapshot, phase: PlaybackPhase, playing: Boolean, playWhenReady: Boolean,
    positionMs: Long, bufferedMs: Long, durationMs: Long, speed: Float,
): PlaybackSnapshot = before.copy(
    playing = playing, playWhenReady = playWhenReady, phase = phase,
    ready = before.ready || phase == PlaybackPhase.Ready,
    ended = when {
        phase == PlaybackPhase.Ready || playing -> false
        phase == PlaybackPhase.Ended && before.phase != PlaybackPhase.Ended -> true
        else -> before.ended
    },
    positionMs = positionMs, bufferedMs = bufferedMs.coerceAtLeast(positionMs),
    durationMs = durationMs.coerceAtLeast(0), speed = speed,
)
