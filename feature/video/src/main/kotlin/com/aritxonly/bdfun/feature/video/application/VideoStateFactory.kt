package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.VideoItem

/** 仓储由组装壳提供；每次创建独立页面控制器，不保存页面或平台对象。 */
class VideoStateFactory(
    private val entries: VideoEntryRepository,
    private val videos: VideoRepository,
    private val playback: VideoPlaybackRepository,
    private val metadata: VideoMetadataRepository,
    private val actions: VideoActionsRepository,
    private val history: PlaybackHistoryRepository,
    private val sponsors: SponsorRepository,
) {
    fun entry(snapshot: VideoItem?) = VideoEntryStateHolder(entries, snapshot)
    fun detail(related: List<VideoItem>) = VideoDetailStateHolder(videos, related, playback)
    fun interaction() = VideoInteractionStateHolder(actions)
    fun metadata() = VideoMetadataStateHolder(metadata)
    fun tracking() = PlaybackTrackingStateHolder(history, sponsors)
}
