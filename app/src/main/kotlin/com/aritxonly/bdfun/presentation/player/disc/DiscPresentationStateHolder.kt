package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

internal class DiscPresentationStateHolder(cover: String) {
    var pressed by mutableStateOf(false)
    var longPressActive by mutableStateOf(false)
    var dragTarget by mutableStateOf(Offset.Zero)
    var spin by mutableFloatStateOf(0f)
    var pressGen by mutableIntStateOf(0)
    var exiting by mutableStateOf(false)
    var exitComposeLogged by mutableStateOf(false)
    var displayedCover by mutableStateOf(cover)
    var coverBlur by mutableFloatStateOf(0f)
    var coverTintColor by mutableStateOf(Color.White)
    var breathPhase by mutableFloatStateOf(0f)
}
internal class DiscPointerSession(start: Offset) {
    var total = Offset.Zero
    var last = start
    var dragging = false
    var armed = false
}
