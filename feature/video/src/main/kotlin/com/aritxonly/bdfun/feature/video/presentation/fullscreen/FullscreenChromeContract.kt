package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** 资源归组装壳所有；视频展示只使用传入ID，不反向引用app的R或全局主题。 */
data class FullscreenIconAssets(
    val play: Int, val next: Int, val danmakuOn: Int, val danmakuOff: Int, val danmakuSettings: Int,
    val like: Int, val comment: Int, val favorite: Int, val share: Int,
)
data class FullscreenAppearance(
    val accentColor: Color, val icons: FullscreenIconAssets,
    val showActions: Boolean, val showViewerCount: Boolean, val showTime: Boolean,
)

/** 共用图片请求和仍在迁移的组合由app提供，布局与点击归视频模块。 */
class FullscreenChromeSlots(
    val avatar: @Composable (String, Modifier) -> Unit,
    val audio: @Composable (FullscreenPlayerInfo, FullscreenPlaybackState, FullscreenActions, Modifier) -> Unit,
    val danmaku: @Composable (FullscreenPlayerInfo, () -> Unit) -> Unit,
)
