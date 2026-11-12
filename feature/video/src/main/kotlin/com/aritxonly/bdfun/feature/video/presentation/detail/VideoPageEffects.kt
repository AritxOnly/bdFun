package com.aritxonly.bdfun.ui


import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberVideoActivityEntry(
    snapshot: com.aritxonly.bdfun.core.model.video.VideoItem?, requestedBvid: String?, requestedAid: Long,
    host: VideoActivityHostActions, factory: com.aritxonly.bdfun.feature.video.application.VideoStateFactory,
): com.aritxonly.bdfun.core.model.video.VideoItem? {
    val entry = remember { factory.entry(snapshot) }
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
fun BindVideoActivityQueue(
    video: com.aritxonly.bdfun.core.model.video.VideoItem, ui: VideoActivityPageStateHolder, host: VideoActivityHostActions,
    queue: com.aritxonly.bdfun.core.application.PlaybackQueueStateHolder, autoNextRequest: () -> Int,
) {
    LaunchedEffect(video.bvid) {
        if (!queue.contains(video.bvid)) {
            queue.clear()
        }
    }
    var consumedAutoNext by ui.consumedAutoNext
    LaunchedEffect(autoNextRequest()) {
        val tick = autoNextRequest()
        if (tick == consumedAutoNext) return@LaunchedEffect
        consumedAutoNext = tick
        val next = queue.next() ?: return@LaunchedEffect
        host.openVideo(next)
        host.finish()
    }
}

@Composable
fun BindVideoActivityWindow(
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
