package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.VideoEntryRepository
import com.aritxonly.bdfun.core.model.video.VideoDetail

class BiliVideoEntryRepository(
    private val loadBvid: suspend (String) -> VideoDetail,
    private val loadAid: suspend (Long) -> VideoDetail,
) : VideoEntryRepository {
    override suspend fun byBvid(bvid: String) = biliOutcome { loadBvid(bvid) }
    override suspend fun byAid(aid: Long) = biliOutcome { loadAid(aid) }
}
