package com.aritxonly.bdfun.core.model.video

/** 空降助手片段（来自 BilibiliSponsorBlock 公共 API）。 */
data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String,
    val actionType: String,
    val uuid: String,
)
