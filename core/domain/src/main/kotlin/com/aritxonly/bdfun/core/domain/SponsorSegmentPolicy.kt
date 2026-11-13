package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.SponsorSegment

/** 沿用接口顺序、左闭右开区间；关闭过的片段在本视频内不再提示。 */
fun activeSponsorSegmentAt(segments: List<SponsorSegment>, positionMs: Long, dismissed: Set<String>): SponsorSegment? =
    segments.firstOrNull { segment ->
        segment.uuid !in dismissed && positionMs >= segment.startMs && positionMs < segment.endMs
    }
