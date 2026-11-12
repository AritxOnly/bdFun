package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import kotlinx.coroutines.delay

@Composable
internal fun BindPlayerContinuation(
    playback: PlaybackController,
    fullscreenInfo: FullscreenPlayerInfo,
    onPageSelected: (com.aritxonly.bdfun.data.VideoPage) -> Unit,
    ui: VideoPlayerUiStateHolder,
) {
    val VideoPageBus = LocalVideoPageState.current
    val state = playback.state.collectAsState()
    val ended by remember(playback) { derivedStateOf { state.value.ended } }
    val playerViewRef by ui.playerViewRef
    val fsResizeMode by ui.fsResizeMode
    val fsLoopMode by ui.fsLoopMode
    val fsAutoPlayNext by ui.fsAutoPlayNext
    val fsHasNext = run {
        val pages = fullscreenInfo.pages
        val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        pages.size > 1 && index >= 0 && index + 1 < pages.size
    }
    LaunchedEffect(fsResizeMode) {
        playerViewRef?.resizeMode = fsResizeMode
    }

    // 播完：单集循环 / 列表循环 / 自动连播（有下一集才切）
    LaunchedEffect(ended, fsLoopMode, fsAutoPlayNext, fsHasNext) {
        if (!ended) return@LaunchedEffect
        when {
            fsLoopMode == 1 -> {
                playback.seekTo(0L)
                playback.play()
                playback.acknowledgeEnd()
            }
            fsHasNext && (fsLoopMode == 2 || fsAutoPlayNext) -> {
                onPageSelected(
                    fullscreenInfo.pages[
                        (fullscreenInfo.pages.indexOfFirst { it.cid == fullscreenInfo.currentCid } + 1)
                            .coerceAtMost(fullscreenInfo.pages.lastIndex)
                    ],
                )
            }
            (fsLoopMode == 2 || fsAutoPlayNext) &&
                com.aritxonly.bdfun.data.VideoPlayQueue.hasNext() -> {
                playback.acknowledgeEnd()
                VideoPageBus.requestAutoNext()
            }
        }
    }

    BindPlayerSleepTimer(playback, ui)
}

@Composable
private fun BindPlayerSleepTimer(playback: PlaybackController, ui: VideoPlayerUiStateHolder) {
    val context = LocalContext.current
    var fsSleepAt by ui.fsSleepAt
    // 定时关闭
    LaunchedEffect(fsSleepAt) {
        if (fsSleepAt <= 0L) return@LaunchedEffect
        while (true) {
            val left = fsSleepAt - System.currentTimeMillis()
            if (left <= 0L) {
                playback.pause()
                fsSleepAt = 0L
                context.showToast("定时关闭已生效")
                break
            }
            delay(1000L)
        }
    }

}
