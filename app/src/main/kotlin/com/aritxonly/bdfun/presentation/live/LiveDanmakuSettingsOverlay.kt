package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun LiveDanmakuSettingsOverlay(
    landscape: Boolean,
    onDismiss: () -> Unit,
) {
    if (landscape) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val panelWidth = (maxWidth.value * 0.41f).dp.coerceAtLeast(360.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.25f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.25f),
                            0.58f to Color.Black.copy(alpha = 0.74f),
                            0.68f to Color.Black.copy(alpha = 0.95f),
                            1f to Color.Black,
                        ),
                    )
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(panelWidth)
                    .fillMaxHeight()
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 32.dp, end = 30.dp, top = 30.dp, bottom = 18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DanmakuSettingsContent(
                    fullscreen = true,
                    onInteract = {},
                    liveMode = true,
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0xFF141519))
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DanmakuSettingsContent(
                    fullscreen = false,
                    onInteract = {},
                    liveMode = true,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 播放设置：竖屏底部弹出，横屏从右侧滑入                                 */
/* ------------------------------------------------------------------ */
