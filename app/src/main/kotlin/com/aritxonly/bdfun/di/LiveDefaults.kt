package com.aritxonly.bdfun.di

import com.aritxonly.bdfun.core.model.live.LiveRoomInfo

/** main的离线/未开播回退，按页面创建，不保存全局可变状态。 */
internal fun liveFallbackRoom() = LiveRoomInfo(
    roomId = 7734200L,
    title = "随便聊聊，顺便看看新版本做得怎么样了",
    uname = "阿茶Teak",
    uid = 946974L,
    onlineText = "1.2 万",
    areaText = "虚拟日常",
    following = false,
)
