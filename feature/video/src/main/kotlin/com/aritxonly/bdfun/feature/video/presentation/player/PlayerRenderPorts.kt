package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class PlayerGestureOptions(
    val verticalGesturesEnabled: () -> Boolean,
    val boostDefaultSpeed: () -> Float,
    val centerSwipeEnabled: () -> Boolean,
    val boostSwipeEnabled: () -> Boolean,
)
class PlayerFullscreenVisuals(
    val appearance: FullscreenAppearance, val slots: FullscreenChromeSlots,
    val danmakuOn: Boolean, val backgroundPlay: Boolean,
)

/** app提供原平台动作/资源/共用弹幕适配；模块拥有原绘制与指针树。 */
class PlayerRenderPorts(
    val page: PlayerPageHost,
    val gestures: PlayerGestureOptions,
    val fullscreenActions: FullscreenActions,
    val picture: VideoPictureSettings,
    val surface: PlayerSurfaceSlots,
    val buffering: @Composable (Modifier) -> Unit,
    val cover: @Composable (String, Modifier) -> Unit,
    val inlineAppearance: () -> InlinePlayerAppearance,
    val fullscreenVisuals: @Composable () -> PlayerFullscreenVisuals,
    val danmakuSender: () -> (suspend (String, Int, Int, Int) -> Boolean)?,
    val inlineMore: @Composable () -> Unit,
)
