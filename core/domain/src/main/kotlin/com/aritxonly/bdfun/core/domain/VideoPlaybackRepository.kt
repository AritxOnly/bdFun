package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.*

/** 播放源参数使用已有接口的含义；不在视图层拼装网络请求。 */
data class VideoStreamRequest(
    val detail: VideoDetail,
    val quality: Int,
    val cid: Long = detail.cid,
    val durationSec: Int = detail.duration,
    val codecPreference: String = "auto",
    val cdnService: String = "base",
)

interface VideoPlaybackRepository {
    suspend fun playInfo(request: VideoStreamRequest): Outcome<PlayInfo>
    suspend fun danmaku(cid: Long, durationSec: Int): Outcome<List<DanmakuItem>>
    suspend fun resumePosition(detail: VideoDetail): Outcome<Long>
}
