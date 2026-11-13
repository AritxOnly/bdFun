package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.PlaybackHistoryRepository
import com.aritxonly.bdfun.core.domain.SponsorRepository

class BiliPlaybackTrackingRepository(private val backend: BiliBackend) : PlaybackHistoryRepository, SponsorRepository {
    override suspend fun reportHistory(aid: Long, cid: Long, progressSec: Long) = biliOutcome {
        backend.reportHistory(aid, cid, progressSec); Unit
    }
    override suspend fun segments(bvid: String) = biliOutcome { backend.sponsorSegments(bvid) }
}
