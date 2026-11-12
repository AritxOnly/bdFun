package com.aritxonly.bdfun.ui


import androidx.activity.compose.BackHandler
import com.aritxonly.bdfun.ui.LocalVideoPageState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.aritxonly.bdfun.AppGraph
@Composable
internal fun rememberVideoActivityEntry(
    snapshot: com.aritxonly.bdfun.data.VideoItem?, requestedBvid: String?, requestedAid: Long,
    host: VideoActivityHostActions,
): com.aritxonly.bdfun.data.VideoItem? {
    val entry = remember { AppGraph.videoEntryStateHolder(snapshot) }
    val state by entry.state.collectAsState()
    if (state.video == null) {
        LaunchedEffect(Unit) {
            if (entry.resolve(requestedBvid, requestedAid) ==
                com.aritxonly.bdfun.feature.video.application.VideoEntryResolution.Close) host.finish()
        }
    }
    return state.video
}

@Composable
internal fun BindVideoActivityQueue(
    video: com.aritxonly.bdfun.data.VideoItem, ui: VideoActivityPageStateHolder, host: VideoActivityHostActions,
) {
    val page = LocalVideoPageState.current
    LaunchedEffect(video.bvid) {
        if (!com.aritxonly.bdfun.data.VideoPlayQueue.contains(video.bvid)) {
            com.aritxonly.bdfun.data.VideoPlayQueue.clear()
        }
    }
    var consumedAutoNext by ui.consumedAutoNext
    LaunchedEffect(page.autoNextRequest) {
        val tick = page.autoNextRequest
        if (tick == consumedAutoNext) return@LaunchedEffect
        consumedAutoNext = tick
        val next = com.aritxonly.bdfun.data.VideoPlayQueue.next() ?: return@LaunchedEffect
        host.openVideo(next)
        host.finish()
    }
}

@Composable
internal fun BindVideoActivityWindow(
    immersiveStatus: Boolean, ui: VideoActivityPageStateHolder, host: VideoActivityHostActions,
) {
    var comments by ui.comments
    var danmakuInputOpen by ui.danmakuInputOpen
    LaunchedEffect(immersiveStatus) {
        if (!host.hasWindow()) return@LaunchedEffect
        runCatching { host.applyStatusBarStyle(immersiveStatus) }
        runCatching { host.resyncSystemBars() }
    }
    BackHandler(enabled = comments != null) { comments = null }
    BackHandler(enabled = danmakuInputOpen) { danmakuInputOpen = false }
}
