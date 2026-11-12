package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.player.PlayGlyph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoCollapseOverlay(
    fullscreen: Boolean, collapseProgress: Float, playerBlockHeight: Dp, statusTop: Dp,
    onRestorePlay: () -> Unit,
) {
        // 随着收起进度给整个播放器盖一层粉色遮罩；收满时完全不透明。
        // 遮罩高度就是播放器块高度，所以状态栏那一条也会一起变粉。
        if (!fullscreen && collapseProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(playerBlockHeight)
                    .background(BrandPink.copy(alpha = collapseProgress)),
            )
        }

        // 粉色遮罩快盖满时，在顶部 56dp 里显示播放入口（接近到顶才出现）。
        val collapsedPlayAlpha = ((collapseProgress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        if (!fullscreen && collapsedPlayAlpha > 0.01f) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop)
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { alpha = collapsedPlayAlpha }
                    .clickable(interactionSource = null, indication = null) {
                        onRestorePlay()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // 和播放器控件同一个播放图标（TeakIcon.PlayerPlay），无底色、纯白。
                    PlayGlyph(com.aritxonly.bdfun.ui.TeakIcon.PlayerPlay, tint = Color.White, size = 16.dp)
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "立即播放",
                    color = Color.White,
                    fontSize = 15.sp,
                )
            }
        }
}
