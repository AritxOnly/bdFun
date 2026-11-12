package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem

internal class VideoActivityPageStateHolder(initialAutoNext: Int) {
    val consumedAutoNext = mutableIntStateOf(initialAutoNext)
    val comments = mutableStateOf<VideoDetail?>(null)
    val danmakuInputOpen = mutableStateOf(false)
}

internal data class VideoActivityScene(val video: VideoItem, val commentRequest: Triple<Long, Long, Long>)
