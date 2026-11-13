package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CachePanel(
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
    accentColor: Color,
) {

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader("缓存", onClose)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "选择要缓存的清晰度",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (playback.qualities.isEmpty()) {
            Text(
                text = "暂无可用清晰度",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
            )
        }
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            playback.qualities
                .let { list -> list.filter { it.id >= 64 }.ifEmpty { list } }
                .forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(interactionSource = null, indication = null) {
                            actions.onCacheSelected(item)
                            onClose()
                        }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = qualityMenuLabel(item.label),
                            color = Color.White,
                            fontSize = 15.sp,
                        )
                        if (item.width > 0 && item.height > 0) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${item.width}×${item.height}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Text(
                        text = "缓存",
                        color = accentColor,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
