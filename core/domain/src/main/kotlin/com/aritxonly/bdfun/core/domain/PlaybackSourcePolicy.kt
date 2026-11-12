package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.core.model.video.Quality

data class PlaybackSourceUrls(val videoUrl: String, val audioUrl: String?)
enum class PlaybackRecoveryAction { RetryOriginal, RetryFallback, Fail }

/** 保留旧播放器的null/空串区别与原始地址优先级，不额外清洗返回字段。 */
fun resolvePlaybackSourceUrls(
    quality: Quality?, info: PlayInfo?, usingOriginal: Boolean, usingFallback: Boolean,
): PlaybackSourceUrls? {
    val video = when {
        usingFallback -> info?.fallbackUrl
        usingOriginal -> quality?.rawVideoUrl?.takeIf { it.isNotBlank() }
            ?: info?.rawFallbackUrl?.takeIf { it.isNotBlank() }
            ?: quality?.videoUrl
            ?: info?.fallbackUrl
        else -> quality?.videoUrl ?: info?.fallbackUrl
    } ?: return null
    val audio = when {
        usingFallback -> null
        usingOriginal -> info?.rawAudioUrl?.takeIf { it.isNotBlank() } ?: info?.audioUrl
        else -> info?.audioUrl
    }
    return PlaybackSourceUrls(video, audio)
}

/** 403先试原视频/音轨地址，再试durl；仅有rawFallback不会触发第一步。 */
fun selectPlaybackRecovery(
    quality: Quality?, info: PlayInfo?, usingOriginal: Boolean, usingFallback: Boolean,
): PlaybackRecoveryAction = when {
    !usingOriginal && (!quality?.rawVideoUrl.isNullOrBlank() || !info?.rawAudioUrl.isNullOrBlank()) ->
        PlaybackRecoveryAction.RetryOriginal
    !usingFallback && !info?.fallbackUrl.isNullOrBlank() -> PlaybackRecoveryAction.RetryFallback
    else -> PlaybackRecoveryAction.Fail
}
