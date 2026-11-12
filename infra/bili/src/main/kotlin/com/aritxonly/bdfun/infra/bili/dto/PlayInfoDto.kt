package com.aritxonly.bdfun.infra.bili.dto

/** 只记录服务端播放表示字段，编码/音频选择仍交给原领域策略。 */
internal data class PlaybackRepresentationDto(
    val id: Int, val bandwidth: Long, val codec: String,
    val url: String, val backups: List<String>,
    val width: Int, val height: Int, val frameRate: String,
)

internal data class PlayInfoDto(
    val videos: List<PlaybackRepresentationDto>,
    val audios: List<PlaybackRepresentationDto>,
    val fallback: String, val durationMs: Long, val acceptQuality: List<Int>,
)
