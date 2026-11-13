package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BottomSearchVideoMetrics(video: VideoItem) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 11.dp)
        Spacer(Modifier.width(2.dp))
        Text(
            text = formatCount(video.play),
            color = colors.onBackgroundVariant,
            fontSize = 10.sp,
        )
        Spacer(Modifier.width(6.dp))
        MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 11.dp)
        Spacer(Modifier.width(2.dp))
        Text(
            text = formatCount(video.danmaku),
            color = colors.onBackgroundVariant,
            fontSize = 10.sp,
        )
    }
}
