package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberVideoCollapseHost(): VideoCollapseHost {
    val page = LocalVideoPageState.current
    return remember(page) {
        VideoCollapseHost({ page.commentThreadVisible }, { page.pauseAll() },
            page.dependencies.background::togglePlayPause, { page.playerCollapseScroll = it })
    }
}
