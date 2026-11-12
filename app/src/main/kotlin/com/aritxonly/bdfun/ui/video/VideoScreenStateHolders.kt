package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder
import com.aritxonly.bdfun.feature.video.application.VideoPlaybackSelectionStateHolder
import kotlinx.coroutines.CoroutineScope

internal class VideoScreenStateHolders(
    val controllers: VideoDetailControllers,
    val metadata: VideoMetadataStateHolder,
    val ui: VideoDetailUiStateHolder,
    val description: VideoDescriptionStateHolder,
)

@Composable
internal fun rememberVideoScreenStateHolders(request: VideoScreenRequest): VideoScreenStateHolders {
    val dependencies = LocalVideoPageState.current.dependencies
    val settings = LocalVideoPageState.current.settings
    val readCache = LocalVideoPageState.current.readCache
    val video = request.video
    val detail = remember(video.bvid) { dependencies.controllers.detail(readCache.related(video.bvid).orEmpty()) }
    val selection = remember(video.bvid) {
        val explicit = when {
            request.initialSeekMs > 0L -> request.initialSeekMs
            settings.resumePlayback -> video.startPositionMs
            else -> 0L
        }
        val local = if (explicit <= 0L && settings.resumePlayback) {
            dependencies.progress.resumeFor(video.bvid)
        } else {
            0L
        }
        VideoPlaybackSelectionStateHolder(video.bvid, video.cid, request.defaultQuality,
            if (explicit > 0L) explicit else local)
    }
    val ui = remember { VideoDetailUiStateHolder() }
    val interaction = remember(video.bvid) { dependencies.controllers.interaction() }
    val metadata = remember(video.bvid) { dependencies.controllers.metadata() }
    val description = remember { VideoDescriptionStateHolder() }
    return remember(detail, interaction, selection, metadata, ui, description) {
        VideoScreenStateHolders(VideoDetailControllers(detail, interaction, selection, readCache), metadata, ui, description)
    }
}

@Composable
internal fun rememberVideoDetailActions(
    isLoggedIn: () -> Boolean, request: VideoScreenRequest, holders: VideoScreenStateHolders,
    scope: CoroutineScope, onBack: () -> Unit,
): VideoDetailViewActions {
    val context = LocalContext.current
    val page = LocalVideoPageState.current
    val latestOnBack by rememberUpdatedState(onBack)
    val controllers = holders.controllers
    return remember(request.video.bvid, controllers.detail, controllers.interaction, controllers.selection, scope) {
        VideoDetailViewActions(context, scope, request.video, controllers, holders.ui,
            page, isLoggedIn, request.openedFromVideoPage, { latestOnBack() })
    }
}
