package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class LiveChromeContent(
    val landscape: Boolean,
    val playing: Boolean,
    val danmakuEnabled: Boolean,
    val qualityLabel: String,
    val title: String,
    val paused: Boolean,
)
internal data class LiveChromeActions(
    val onPlayPause: () -> Unit,
    val onReload: () -> Unit,
    val onToggleDanmaku: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenDanmakuSettings: () -> Unit,
    val onToggleFullscreen: () -> Unit,
    val onBack: () -> Unit,
)
internal class LiveChromeStateHolder {
    var visible by mutableStateOf(true)
    var touchTick by mutableIntStateOf(0)
    var dragBase by mutableFloatStateOf(0f)
    var gestureTip by mutableStateOf<LiveGestureTip?>(null)
}
internal class LiveChromeRender(val content: LiveChromeContent, val actions: LiveChromeActions, val ui: LiveChromeStateHolder) {
    val landscape get() = content.landscape
    val playing get() = content.playing
    val danmakuEnabled get() = content.danmakuEnabled
    val qualityLabel get() = content.qualityLabel
    val title get() = content.title
    val paused get() = content.paused
    val onPlayPause get() = actions.onPlayPause
    val onReload get() = actions.onReload
    val onToggleDanmaku get() = actions.onToggleDanmaku
    val onOpenSettings get() = actions.onOpenSettings
    val onOpenDanmakuSettings get() = actions.onOpenDanmakuSettings
    val onToggleFullscreen get() = actions.onToggleFullscreen
    val onBack get() = actions.onBack
    var visible: Boolean
        get() = ui.visible
        set(value) { ui.visible = value }
    var dragBase: Float
        get() = ui.dragBase
        set(value) { ui.dragBase = value }
    var gestureTip: LiveGestureTip?
        get() = ui.gestureTip
        set(value) { ui.gestureTip = value }
    val touchTick get() = ui.touchTick
    fun touch() { ui.touchTick++ }
}
internal data class LiveGestureTip(val brightness: Boolean, val value: Float)
