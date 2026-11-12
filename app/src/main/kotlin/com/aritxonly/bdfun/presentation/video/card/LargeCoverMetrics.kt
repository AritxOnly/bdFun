package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.LargeCoverMetrics(video: VideoItem) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (video.duration > 0) {
            Text(
                text = formatDuration(video.duration),
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 12.sp,
            )
            Spacer(Modifier.width(9.dp))
        }
        if (video.play > 0 || video.danmaku > 0) {
            MetricGlyph(play = true, size = 13.dp)
            Spacer(Modifier.width(3.dp))
            Text(
                text = formatCount(video.play),
                color = Color.White,
                fontSize = 10.sp,
            )
            Spacer(Modifier.width(9.dp))
            MetricGlyph(play = false, size = 13.dp)
            Spacer(Modifier.width(3.dp))
            Text(
                text = formatCount(video.danmaku),
                color = Color.White,
                fontSize = 10.sp,
            )
        }
    }

}
