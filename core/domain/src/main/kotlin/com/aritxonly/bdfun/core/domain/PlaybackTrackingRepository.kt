package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.SponsorSegment

interface PlaybackHistoryRepository {
    suspend fun reportHistory(aid: Long, cid: Long, progressSec: Long): Outcome<Unit>
}
interface SponsorRepository {
    suspend fun segments(bvid: String): Outcome<List<SponsorSegment>>
}

/** 保留首次 3 秒门槛、15 秒周期和结束时的 3 秒补报周期。 */
object PlaybackHistoryPolicy {
    fun shouldReport(playing: Boolean, ended: Boolean, positionMs: Long, nowMs: Long, lastAtMs: Long): Boolean =
        (playing || ended) && positionMs >= 3000L &&
            (nowMs - lastAtMs >= 15000L || (ended && nowMs - lastAtMs >= 3000L))
}
