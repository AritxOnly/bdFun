package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.aritxonly.bdfun.BrandPink

@Composable
internal fun SubtitlesPanel(
    info: FullscreenPlayerInfo,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader("字幕", onClose)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (info.selectedSubtitle == null) {
                        BrandPink.copy(alpha = 0.18f)
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(interactionSource = null, indication = null) {
                    actions.onSubtitleSelected(null)
                    onClose()
                }
                .padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("关闭字幕", color = Color.White, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            if (info.selectedSubtitle == null) CheckGlyph(BrandPink)
        }
        if (info.subtitles.isEmpty()) {
            Text(
                text = "该视频暂无字幕",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 18.dp),
            )
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                info.subtitles.forEach { track ->
                    val active = info.selectedSubtitle?.id == track.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
                            )
                            .clickable(interactionSource = null, indication = null) {
                                actions.onSubtitleSelected(track)
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.label.ifBlank { "字幕" },
                                color = if (active) BrandPink else Color.White,
                                fontSize = 15.sp,
                            )
                            if (track.isAi) {
                                Spacer(Modifier.height(2.dp))
                                Text("AI 字幕", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            }
                        }
                        if (active) CheckGlyph(BrandPink)
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------ audio mode */
