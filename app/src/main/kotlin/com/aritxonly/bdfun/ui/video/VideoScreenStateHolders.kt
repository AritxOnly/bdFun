package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder
import com.aritxonly.bdfun.feature.video.application.VideoPlaybackSelectionStateHolder
import kotlinx.coroutines.CoroutineScope

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
