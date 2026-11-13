package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem

internal data class BottomSearchRequest(val query: String, val submitted: String?)
internal data class BottomSearchTransition(
    val reveal: Float, val windowAlpha: Float, val maskAlpha: Float, val contentAlpha: Float,
)
internal data class BottomSearchActions(
    val onQueryChange: (String) -> Unit, val onSubmit: (String) -> Unit,
    val onCancel: () -> Unit, val onBackToLanding: () -> Unit,
    val onOpenVideo: (VideoItem) -> Unit, val onOpenUser: (SearchUser) -> Unit,
)
internal class BottomSearchPresentationState(history: List<String>) {
    var tab by mutableIntStateOf(0)
    var history by mutableStateOf(history)
}
