package com.aritxonly.bdfun.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable

@Composable
internal fun VideoScreen(
    isLoggedIn: () -> Boolean, request: VideoScreenRequest, presentation: VideoScreenPresentation, events: VideoScreenEvents,
    pager: PagerState = rememberPagerState(pageCount = { 2 }),
) {
    VideoDetailEntry(isLoggedIn, request, presentation, events, pager, videoDetailScreenPorts())
}
