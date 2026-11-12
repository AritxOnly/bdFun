package com.aritxonly.bdfun.core.navigation

import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.feed.DynamicItem

sealed interface Route {
    data class Video(
        val bvid: String, val aid: Long = 0, val snapshot: VideoItem? = null,
        val fromVideoPage: Boolean = false,
        val commentRoot: Long = 0, val commentTarget: Long = 0, val seekMs: Long = 0,
    ) : Route
    data class Space(val mid: Long) : Route
    data class Dynamic(val id: String, val snapshot: DynamicItem? = null) : Route
    data class PrivateChat(
        val talkerId: Long, val sessionType: Int = 1, val name: String = "", val avatar: String = "",
        val unread: Int = 0, val maxSeqno: Long = 0,
    ) : Route
}
