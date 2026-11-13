package com.aritxonly.bdfun.core.model.video

/** 字幕单句。 */
data class SubtitleCue(
    val fromMs: Long,
    val toMs: Long,
    val text: String,
)
