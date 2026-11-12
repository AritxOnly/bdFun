package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.PlayInfo

data class InlineVideoPreferences(val quality: Int, val codecPreference: String, val cdnService: String)

interface InlineVideoRepository {
    fun cachedPlayInfo(bvid: String): PlayInfo?
    /** 设置在详情请求结束、播放缓存仍未命中时采样，沿用原请求时序。 */
    suspend fun load(bvid: String, preferences: () -> InlineVideoPreferences): Outcome<PlayInfo>
}
