package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackSource
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform
import androidx.compose.ui.platform.LocalContext

@Composable
fun BindPlayerSource(
    playback: VideoPlaybackPlatform,
    media: VideoMediaInput,
    presentation: VideoPlayerPresentation,
    ui: VideoPlayerUiStateHolder, diagnostics: PlayerDiagnostics, autoPlay: () -> Boolean,
) {
    val log = diagnostics.log
    val bvid = media.bvid
    val info = media.info
    val quality = media.quality
    val autoQuality = media.autoQuality
    val resumePositionMs = presentation.resumePositionMs
    val usingFallback by ui.usingFallback
    val usingOriginal by ui.usingOriginal
    val speed by ui.speed
    LaunchedEffect(info, quality, usingFallback, usingOriginal) {
        val url = when {
            usingFallback -> info?.fallbackUrl
            usingOriginal -> quality?.rawVideoUrl?.takeIf { it.isNotBlank() }
                ?: info?.rawFallbackUrl?.takeIf { it.isNotBlank() }
                ?: quality?.videoUrl
                ?: info?.fallbackUrl
            else -> quality?.videoUrl ?: info?.fallbackUrl
        } ?: run {
            log(
                "PLAYER",
                "no-source bvid=$bvid q=${quality?.id} hasInfo=${info != null} " +
                    "qualities=${info?.qualities?.size ?: 0} " +
                    "usingOriginal=$usingOriginal usingFallback=$usingFallback",
            )
            if (info != null && info.qualities.isEmpty() && info.fallbackUrl.isNullOrBlank()) {
                diagnostics.toast("播放地址为空，已记录日志")
            }
            return@LaunchedEffect
        }
        val audioUrl = when {
            usingFallback -> null
            usingOriginal -> info?.rawAudioUrl?.takeIf { it.isNotBlank() } ?: info?.audioUrl
            else -> info?.audioUrl
        }
        log(
            "PLAYER",
            "source bvid=$bvid q=${quality?.id} auto=$autoQuality " +
                "usingOriginal=$usingOriginal usingFallback=$usingFallback " +
                "video=${describeMediaUrl(url)} audio=${describeMediaUrl(audioUrl)}",
        )
        playback.prepare(PlaybackSource(
            videoUrl = url, audioUrl = audioUrl,
            headers = mapOf("Referer" to "https://www.bilibili.com/video/$bvid/"), userAgent = playback.userAgent,
            resumePositionMs = resumePositionMs, autoPlay = autoPlay(), speed = speed,
        ))
    }

}
