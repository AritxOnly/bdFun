package com.aritxonly.bdfun.core.model.video

/** 官方字幕轨（`/x/player/v2` 的 subtitle.subtitles）。 */
data class SubtitleTrack(
    val id: String,
    val label: String,
    val url: String,
    val isAi: Boolean = false,
)
