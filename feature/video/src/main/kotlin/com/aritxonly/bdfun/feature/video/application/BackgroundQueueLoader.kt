package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.domain.videoPlaybackPages
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*

data class BackgroundTrack(val detail: VideoDetail, val cid: Long, val audioUrl: String)

/** 后台选集仍走已有详情与播放源仓储；新选集/退出会使旧请求失效。 */
class BackgroundQueueLoader(
    private val videos: VideoRepository,
    private val playback: VideoPlaybackRepository,
) {
    private var revision = 0L
    fun invalidate() { revision++ }

    suspend fun skip(
        detail: VideoDetail, cid: Long, delta: Int, quality: Int, codec: String, cdn: String,
    ): BackgroundTrack? {
        val version = ++revision
        val pages = videoPlaybackPages(detail)
        val index = pages.indexOfFirst { it.cid == cid }
        if (index < 0) return null
        val target = pages.getOrNull(index + delta) ?: return null
        val episode = detail.episodes.firstOrNull { it.cid == target.cid }
        val loaded = if (episode != null && episode.bvid.isNotBlank() && episode.bvid != detail.bvid) {
            (videos.videoDetail(episode.bvid) as? Outcome.Success)?.value ?: return null
        } else detail
        if (version != revision) return null
        val result = playback.playInfo(VideoStreamRequest(loaded, quality, target.cid, target.duration, codec, cdn))
        if (version != revision) return null
        val info = (result as? Outcome.Success)?.value ?: return null
        val audio = info.audioUrl ?: info.fallbackUrl ?: info.qualities.firstOrNull()?.videoUrl ?: return null
        return BackgroundTrack(loaded, target.cid, audio)
    }

}
