package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.GridCoverMetrics(video: VideoItem, reserveMeta: Boolean) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 历史记录等没有播放/弹幕数据的来源就不显示这两个 0
                if (reserveMeta || video.play > 0 || video.danmaku > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetricGlyph(play = true)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (reserveMeta && video.play <= 0) "—" else formatCount(video.play),
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Normal,
                        )
                        Spacer(Modifier.width(6.dp))
                        MetricGlyph(play = false)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (reserveMeta && video.danmaku <= 0) "—" else formatCount(video.danmaku),
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (video.duration > 0) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                    )
                }
            }

}
