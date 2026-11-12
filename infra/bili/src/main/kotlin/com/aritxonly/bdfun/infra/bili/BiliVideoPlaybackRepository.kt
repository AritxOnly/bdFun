package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.VideoPlaybackRepository
import com.aritxonly.bdfun.core.domain.VideoStreamRequest
import com.aritxonly.bdfun.core.model.video.*

class BiliVideoPlaybackRepository(
    private val loadPlayInfo: suspend (VideoStreamRequest) -> PlayInfo,
    private val loadDanmaku: suspend (Long, Int) -> List<DanmakuItem>,
    private val loadResumePosition: suspend (VideoDetail) -> Long,
) : VideoPlaybackRepository {
    override suspend fun playInfo(request: VideoStreamRequest) = biliOutcome { loadPlayInfo(request) }
    override suspend fun danmaku(cid: Long, durationSec: Int) = biliOutcome { loadDanmaku(cid, durationSec) }
    override suspend fun resumePosition(detail: VideoDetail) = biliOutcome { loadResumePosition(detail) }
}
