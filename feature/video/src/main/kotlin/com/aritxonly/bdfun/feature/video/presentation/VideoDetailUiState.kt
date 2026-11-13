package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableStateOf

/** 页面瞬时控制；真实播放、请求和互动状态仍由各应用层容器发布。 */
class VideoDetailUiStateHolder {
    val fullscreen = mutableStateOf(false)
    val playing = mutableStateOf(true)
    val metaReady = mutableStateOf(false)
    val coinSheet = mutableStateOf(false)
    val favSheet = mutableStateOf(false)
}
