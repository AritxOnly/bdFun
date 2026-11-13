package com.aritxonly.bdfun.feature.sent.application

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.model.user.SentRecord
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SentHistoryState(val records: List<SentRecord>, val filter: Int = 0) {
    val shown get() = when (filter) {
        1 -> records.filter { it.kind == "comment" }
        2 -> records.filter { it.kind == "danmaku" }
        else -> records
    }
    fun count(index: Int) = when (index) {
        1 -> records.count { it.kind == "comment" }
        2 -> records.count { it.kind == "danmaku" }
        else -> records.size
    }
}
/** 保持进入页面时的一次快照，不新增 resume 刷新或实时记录订阅。 */
internal class SentHistoryStateHolder(records: List<SentRecord>) {
    private val mutable = MutableStateFlow(SentHistoryState(records.toList()))
    val state = mutable.asStateFlow()
    fun select(filter: Int) { mutable.update { it.copy(filter = filter) } }
    fun videoFor(record: SentRecord): VideoItem? = if (record.bvid.isBlank()) null else VideoItem(
        bvid = record.bvid,
        aid = 0,
        title = record.title,
        cover = "",
        ownerName = "",
        ownerFace = "",
        ownerMid = 0,
        play = 0,
        danmaku = 0,
        duration = 0,
        pubdate = 0,
        extras = VideoItemExtras(
            startPositionMs = record.videoTimeMs,
        ),
    )
}
