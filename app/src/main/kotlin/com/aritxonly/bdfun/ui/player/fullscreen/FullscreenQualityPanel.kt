package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink

@Composable
internal fun QualityPanel(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {

    PanelCard(title = "画质") {
        if (playback.qualities.isEmpty()) {
                Text("暂无可用画质", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                AutomaticQualityRow(playback.autoQuality, actions, onClose)
                playback.qualities
                    .let { list ->
                        // 和半屏播放器一致：不列 720P 以下的档位；
                        // 视频本身最高不到 720P 时退回完整列表，避免菜单空掉。
                        list.filter { it.id >= 64 }.ifEmpty { list }
                    }
                    .forEach { item ->
                    val active = !playback.autoQuality && item.id == playback.qualityId
                    QualityOptionRow(item, active, actions, onClose)
                }
            }
        }
}


@Composable
private fun AutomaticQualityRow(active: Boolean, actions: FullscreenActions, onClose: () -> Unit) {
    // 自动：按当前网速挑一档；选中后画质标签显示「自动」
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
            )
            .clickable(interactionSource = null, indication = null) {
                actions.onQualitySelected(0)
                onClose()
            }
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "自动",
            color = if (active) BrandPink else Color.White,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (active) CheckGlyph(BrandPink)
    }
}

@Composable
private fun QualityOptionRow(item: com.aritxonly.bdfun.data.Quality, active: Boolean, actions: FullscreenActions, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
            )
            .clickable(interactionSource = null, indication = null) {
                actions.onQualitySelected(item.id)
                onClose()
            }
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = qualityMenuLabel(item.label),
                color = if (active) BrandPink else Color.White,
                fontSize = 15.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
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
        if (active) CheckGlyph(BrandPink)
    }
}
