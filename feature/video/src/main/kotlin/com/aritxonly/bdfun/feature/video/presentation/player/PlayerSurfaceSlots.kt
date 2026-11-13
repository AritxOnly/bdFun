package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aritxonly.bdfun.core.model.video.DanmakuItem

/** 保留原弹幕适配入口，视频模块不跨层读取app设置或构造弹幕引擎。 */
class PlayerSurfaceSlots(
    val danmaku: @Composable (List<DanmakuItem>, () -> Long, Boolean, Float, Modifier, Boolean) -> Unit,
)
data class VideoPictureSettings(val progressBarMode: Int, val hotDanmakuEnabled: Boolean, val accentColor: Color)
