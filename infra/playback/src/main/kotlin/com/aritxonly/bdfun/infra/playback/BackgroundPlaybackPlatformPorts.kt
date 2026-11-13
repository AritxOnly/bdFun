package com.aritxonly.bdfun.infra.playback

import android.app.Service
import com.aritxonly.bdfun.core.domain.PlaybackSettings

/** app注入原服务组件、事件时设置与诊断，不让平台层依赖AppGraph/feature。 */
class BackgroundPlaybackPlatformPorts(
    val serviceClass: Class<out Service>,
    val startAction: String,
    val settings: () -> PlaybackSettings,
    val userAgent: String,
    val debug: Boolean,
    val debugLog: (String, String) -> Unit,
    val trace: (String, String) -> Unit,
)
