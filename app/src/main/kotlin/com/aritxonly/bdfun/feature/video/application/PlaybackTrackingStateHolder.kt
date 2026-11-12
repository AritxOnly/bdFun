package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.domain.shouldReportPlaybackHistory
import com.aritxonly.bdfun.core.domain.activeSponsorSegmentAt
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.application.PlaybackSnapshot
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class PlaybackTrackingState(
    val bvid: String = "", val segments: List<SponsorSegment> = emptyList(),
    val reporting: Boolean = false, val lastReportedAtMs: Long = 0,
)

/** 历史请求不阻塞进度轮询；片段按 bvid/开关版本隔离，协程取消继续传播。 */
internal class PlaybackTrackingStateHolder(
    private val history: PlaybackHistoryRepository, private val sponsors: SponsorRepository,
) {
    private val mutableState = MutableStateFlow(PlaybackTrackingState())
    val state = mutableState.asStateFlow()
    private var segmentRevision = 0L
    fun activeSegment(snapshot: PlaybackTrackingState, bvid: String, positionMs: Long, dismissed: Set<String>): SponsorSegment? =
        activeSponsorSegmentAt(if (snapshot.bvid == bvid) snapshot.segments else emptyList(), positionMs, dismissed)

    suspend fun loadSegments(bvid: String, enabled: Boolean) {
        val version = ++segmentRevision
        if (!enabled) {
            mutableState.update { it.copy(bvid = bvid, segments = emptyList()) }
            return
        }
        val result = sponsors.segments(bvid)
        if (version == segmentRevision) mutableState.update { it.copy(bvid = bvid,
            segments = (result as? Outcome.Success)?.value.orEmpty()) }
    }
    suspend fun reportIfDue(aid: Long, cid: Long, playback: PlaybackSnapshot, nowMs: Long) {
        val before = state.value
        if (before.reporting || !shouldReportPlaybackHistory(playback.playing, playback.ended,
                playback.positionMs, nowMs, before.lastReportedAtMs)) return
        mutableState.update { it.copy(reporting = true, lastReportedAtMs = nowMs) }
        try { history.reportHistory(aid, cid, playback.positionMs / 1000) }
        finally { mutableState.update { it.copy(reporting = false) } }
    }
}
