package com.aritxonly.bdfun.feature.video.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class VideoPlaybackSelection(
    val bvid: String, val cid: Long, val quality: Int,
    val autoQuality: Boolean = quality == 0, val resumePositionMs: Long = 0,
)

/** 显式定位 > 本地进度 > 云端进度；切集清零，手动换档延续当前播放位置。 */
internal class VideoPlaybackSelectionStateHolder(bvid: String, cid: Long, quality: Int, initialPositionMs: Long) {
    private val mutableState = MutableStateFlow(VideoPlaybackSelection(bvid, cid, quality, resumePositionMs = initialPositionMs))
    val state = mutableState.asStateFlow()

    fun selectQuality(quality: Int, positionMs: Long): Boolean {
        val before = mutableState.value
        if ((quality == 0 && before.autoQuality) || (quality != 0 && quality == before.quality)) return false
        mutableState.update { it.copy(quality = quality, autoQuality = quality == 0, resumePositionMs = positionMs) }
        return true
    }

    fun selectPage(cid: Long, bvid: String = mutableState.value.bvid) {
        mutableState.update { it.copy(bvid = bvid, cid = cid, resumePositionMs = 0L) }
    }

    fun restoreCid(cid: Long) { mutableState.update { it.copy(cid = cid) } }
    fun seek(positionMs: Long) { mutableState.update { it.copy(resumePositionMs = positionMs) } }
    fun offerCloudResume(positionMs: Long) {
        mutableState.update {
            if (it.resumePositionMs <= 0L && positionMs > 0L) it.copy(resumePositionMs = positionMs) else it
        }
    }
}
