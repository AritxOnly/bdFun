package com.aritxonly.bdfun.playback

import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.core.domain.PlaybackSettings
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.FreezeWatchdog
import com.aritxonly.bdfun.data.LogCollector
import com.aritxonly.bdfun.infra.playback.BackgroundPlaybackPlatformPorts
import com.aritxonly.bdfun.ui.BackgroundPlaybackService

internal fun backgroundPlaybackPorts(settings: () -> PlaybackSettings) = BackgroundPlaybackPlatformPorts(
    BackgroundPlaybackService::class.java, BackgroundPlaybackService.ACTION_START, settings, BiliApi.UA,
    BuildConfig.DEBUG, LogCollector::record, FreezeWatchdog::log,
)
