package com.aritxonly.bdfun.core.model.playback

/** CDN 选择值；URL 识别与改写由领域策略和平台适配器共同完成。 */
data class CdnService(
    val id: String, val label: String, val host: String? = null,
    val useBackup: Boolean = false, val auto: Boolean = false,
)
