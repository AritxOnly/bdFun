package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun VideoPlayer(
    media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, modifier: Modifier = Modifier,
) {
    VideoPlayerEntry(media, presentation, actions, modifier, rememberPlayerEntryPorts())
}
