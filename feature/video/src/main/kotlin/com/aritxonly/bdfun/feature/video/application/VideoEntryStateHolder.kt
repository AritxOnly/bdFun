package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.VideoEntryRepository
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VideoEntryState(
    val video: VideoItem?, val loading: Boolean, val error: AppError? = null,
)
enum class VideoEntryResolution { Ready, Close, Obsolete }

/** 入口快照优先；BV/AV 回包映射沿用 Activity 原字段，页面只处理关闭与显示。 */
class VideoEntryStateHolder(private val repository: VideoEntryRepository, snapshot: VideoItem? = null) {
    private val initialSnapshot = snapshot
    private val mutableState = MutableStateFlow(VideoEntryState(snapshot, snapshot == null))
    val state = mutableState.asStateFlow()
    private var revision = 0L

    suspend fun resolve(bvid: String?, aid: Long): VideoEntryResolution {
        if (initialSnapshot != null) return VideoEntryResolution.Ready
        val version = ++revision
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            val result = when {
                !bvid.isNullOrBlank() -> repository.byBvid(bvid)
                aid > 0L -> repository.byAid(aid)
                else -> Outcome.Failure(AppError.Unknown("缺少视频标识"))
            }
            if (version != revision) return VideoEntryResolution.Obsolete
            return when (result) {
                is Outcome.Success -> {
                    mutableState.update { it.copy(
                        video = entryVideo(result.value),
                        error = null,
                    ) }
                    VideoEntryResolution.Ready
                }
                is Outcome.Failure -> {
                    mutableState.update { it.copy(error = result.error) }
                    VideoEntryResolution.Close
                }
            }
        } finally {
            if (version == revision) mutableState.update { it.copy(loading = false) }
        }
    }

    private fun entryVideo(detail: VideoDetail) = VideoItem(
        bvid = detail.bvid,
        aid = detail.aid,
        title = detail.title,
        cover = detail.cover,
        ownerName = detail.ownerName,
        ownerFace = detail.ownerFace,
        ownerMid = detail.ownerMid,
        play = detail.play,
        danmaku = detail.danmaku,
        duration = detail.duration,
        pubdate = detail.pubdate,
        extras = VideoItemExtras(
            cid = detail.cid,
        ),
    )
}
