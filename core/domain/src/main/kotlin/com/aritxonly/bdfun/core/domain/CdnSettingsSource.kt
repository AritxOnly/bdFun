package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.playback.CdnService

/** 设置页只观察测速状态，不接触下载客户端与持久化实现。 */
interface CdnSettingsSource {
    val services: List<CdnService>
    fun speeds(): Map<String, Double>
    fun currentServiceId(): String?
    fun isFresh(): Boolean
    fun isProbing(): Boolean
    suspend fun refresh(force: Boolean): Outcome<Boolean>
}
