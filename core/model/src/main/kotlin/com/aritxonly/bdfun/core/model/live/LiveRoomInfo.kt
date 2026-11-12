package com.aritxonly.bdfun.core.model.live

data class LiveRoomInfo(
    val roomId: Long,
    val title: String,
    val uname: String,
    val uid: Long,
    val face: String = "",
    val cover: String = "",
    /** 人气/在线人数，已格式化好，例如「1.2 万」。 */
    val onlineText: String = "",
    /** 分区，例如「虚拟日常」。 */
    val areaText: String = "",
    /** 是否已关注主播。 */
    val following: Boolean = false,
    /** 是否正在直播。 */
    val live: Boolean = true,
)
