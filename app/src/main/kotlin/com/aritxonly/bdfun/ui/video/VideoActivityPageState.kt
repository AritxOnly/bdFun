package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.data.VideoItem

internal data class VideoActivityScene(val video: VideoItem, val commentRequest: Triple<Long, Long, Long>)
