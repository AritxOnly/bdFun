package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.DownloadQueueStateHolder
import com.aritxonly.bdfun.core.domain.PlaybackProgressRepository
import com.aritxonly.bdfun.core.domain.PlaybackSettings
import com.aritxonly.bdfun.feature.video.application.VideoReadCacheScope
import com.aritxonly.bdfun.feature.video.application.VideoStateFactory

/** 保留下载延迟取值和设置的事件时读取，由app提供原实例。 */
class VideoDetailLoadPorts(
    val downloads: () -> DownloadQueueStateHolder,
    val settings: PlaybackSettings,
    val log: (String, String) -> Unit,
)

class VideoDetailStatePorts(
    val controllers: VideoStateFactory,
    val readCache: VideoReadCacheScope,
    val progress: PlaybackProgressRepository,
    val settings: PlaybackSettings,
)
