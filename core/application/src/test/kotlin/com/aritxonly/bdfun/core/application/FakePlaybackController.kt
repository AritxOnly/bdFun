package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 用真实契约验证所有权与交接；不依赖 Android 或网络。 */
class FakePlaybackController : PlaybackController {
    private val mutableState = MutableStateFlow(PlaybackSnapshot())
    override val state = mutableState.asStateFlow()
    val commands = mutableListOf<String>()
    var released = false
    fun publish(value: PlaybackSnapshot) { mutableState.value = value }
    override fun refresh() = state.value
    override fun prepare(source: PlaybackSource) {
        commands += "prepare"
        mutableState.value = PlaybackSnapshot(playing = source.autoPlay, playWhenReady = source.autoPlay,
            positionMs = source.resumePositionMs, speed = source.speed, phase = PlaybackPhase.Ready, ready = true)
    }
    override fun play() { commands += "play"; mutableState.value = state.value.copy(playing = true, playWhenReady = true) }
    override fun pause() { commands += "pause"; mutableState.value = state.value.copy(playing = false, playWhenReady = false) }
    override fun seekTo(positionMs: Long) { commands += "seek"; mutableState.value = state.value.copy(positionMs = positionMs) }
    override fun setSpeed(speed: Float) { commands += "speed"; mutableState.value = state.value.copy(speed = speed) }
    override fun acknowledgeEnd() { mutableState.value = state.value.copy(ended = false) }
    override fun release() { released = true; mutableState.value = PlaybackSnapshot() }
    override fun addListener(listener: PlaybackController.Listener) {}
    override fun removeListener(listener: PlaybackController.Listener) {}
}
