package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

class VideoActivityPageStateHolder(initialAutoNext: Int) {
    val consumedAutoNext = mutableIntStateOf(initialAutoNext)
    val comments = mutableStateOf<VideoDetail?>(null)
    val danmakuInputOpen = mutableStateOf(false)
}
