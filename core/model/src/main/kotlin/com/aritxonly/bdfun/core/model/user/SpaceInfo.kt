package com.aritxonly.bdfun.core.model.user

import com.aritxonly.bdfun.core.model.video.VideoItem

data class SpaceInfo(
    val profile: UserProfile,
    val videos: List<VideoItem>,
    val videoCount: Int = 0,
    val isFollowed: Boolean = false,
)
