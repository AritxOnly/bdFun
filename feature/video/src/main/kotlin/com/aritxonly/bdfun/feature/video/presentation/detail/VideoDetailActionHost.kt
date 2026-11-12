package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.application.DownloadQueueStateHolder
import com.aritxonly.bdfun.core.domain.PlaybackSettings

/** 页面动作不持有Android对象，平台提示/分享/交接仍由app执行。 */
interface VideoDetailActionHost {
    val settings: PlaybackSettings
    val downloads: DownloadQueueStateHolder
    var progressMs: Long
    fun showToast(text: String)
    fun share(text: String)
    fun requestHandOff()
}
