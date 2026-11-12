package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackPhase
import com.aritxonly.bdfun.core.application.PlaybackFailure
import com.aritxonly.bdfun.data.PlaybackProgressStore

@Composable
internal fun BindPlayerObserver(
    playback: PlaybackController,
    media: VideoMediaInput,
    actions: VideoPlayerActions,
    ui: VideoPlayerUiStateHolder,
) {
    val settings = LocalVideoPageState.current.settings
    val context = LocalContext.current
    val PlayerUiState = LocalVideoPageState.current.playerUi
    val bvid = media.bvid
    val info = media.info
    val quality = media.quality
    val latestBvid by rememberUpdatedState(bvid)
    val onPlayStateChanged = actions.transport.onPlayStateChanged
    var firstFrame by ui.firstFrame
    var controlsVisible by ui.controlsVisible
    val latestInfo by rememberUpdatedState(info)
    val latestQuality by rememberUpdatedState(quality)
    DisposableEffect(Unit) {
        val listener = object : PlaybackController.Listener {
            override fun onRenderedFirstFrame() {
                firstFrame = true
                com.aritxonly.bdfun.data.LogCollector.record(
                    "PLAYER",
                    "first-frame bvid=$bvid q=${latestQuality?.id}",
                )
            }

            override fun onPlayingChanged(isPlaying: Boolean) {
                onPlayStateChanged(isPlaying)
            }

            override fun onPhaseChanged(state: PlaybackPhase) {
                if (state == PlaybackPhase.Buffering || state == PlaybackPhase.Ready) {
                    com.aritxonly.bdfun.data.LogCollector.record(
                        "PLAYER",
                        "state=${if (state == PlaybackPhase.Buffering) "buffering" else "ready"} " +
                            "bvid=$bvid q=${latestQuality?.id}",
                    )
                }
                if (state == PlaybackPhase.Ready) {
                    playback.acknowledgeEnd()
                }
                if (state == PlaybackPhase.Ended) {
                    controlsVisible = true
                    PlayerUiState.controlsVisible = true
                }
            }

            override fun onError(error: PlaybackFailure) {
                handlePlayerError(context, error, latestQuality, latestInfo, ui)
            }
        }
        playback.addListener(listener)
        onDispose {
            if (settings.resumePlayback) {
                runCatching {
                    PlaybackProgressStore.save(latestBvid, playback.refresh().positionMs, playback.refresh().durationMs)
                }
            }
            playback.removeListener(listener)
            val handOff = settings.showDiscAfterVideo && !settings.stopOnVideoPageExit &&
                BackgroundPlayback.shouldHandOff() &&
                playback.refresh().phase != PlaybackPhase.Idle
            if (handOff) {
                // 页面正常关闭，但播放器继续活着：系统一镜到底退出动画照常播。
                BackgroundPlayback.adopt(playback, context)
            } else {
                BackgroundPlayback.cancelHandOff()
                playback.release()
            }
        }
    }

}

private fun handlePlayerError(
    context: android.content.Context,
    error: PlaybackFailure,
    latestQuality: com.aritxonly.bdfun.data.Quality?,
    latestInfo: com.aritxonly.bdfun.data.PlayInfo?,
    ui: VideoPlayerUiStateHolder,
) {
    var usingOriginal by ui.usingOriginal
    var usingFallback by ui.usingFallback
    com.aritxonly.bdfun.data.LogCollector.record(
        "PLAYER",
        "error code=${error.code} q=${latestQuality?.id} " +
            "usingOriginal=$usingOriginal usingFallback=$usingFallback " +
            "message=${error.message?.take(300)}",
    )
    // CDN 节点有时会拒绝带签名的 4K/音轨地址（403），先用原地址重试，再考虑降画质。
    val rawVideo = latestQuality?.rawVideoUrl.orEmpty()
    val rawAudio = latestInfo?.rawAudioUrl.orEmpty()
    if (!usingOriginal && (rawVideo.isNotBlank() || rawAudio.isNotBlank())) {
        usingOriginal = true
        return
    }
    // DASH 高画质在部分 HyperOS / 解码器组合上会失败；优先退回 durl 兜底流。
    if (!usingFallback && !latestInfo?.fallbackUrl.isNullOrBlank()) {
        usingFallback = true
        context.showToast("当前清晰度播放失败，已切换到兼容画质")
        return
    }
    context.showToast("播放失败，请尝试切换清晰度")
}
