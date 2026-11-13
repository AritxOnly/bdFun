package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.aritxonly.bdfun.feature.video.application.*

@Composable
internal fun DanmakuInputLayout(runtime: DanmakuInputRuntime, reveal: Float, bottomDp: Float, modifier: Modifier) {
    val showEmoji = runtime.ui.showEmoji
    val failed = runtime.form.failed
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 22f
                val s = 0.985f + 0.015f * reveal
                scaleX = s
                scaleY = s
            }
            .padding(bottom = bottomDp.dp, start = 12.dp, end = 12.dp)
            .padding(vertical = 8.dp),
    ) {
        if (showEmoji) {
            EmojiPanel(
                onPick = { runtime.controller.append(it) },
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DanmakuInputSurface(runtime)
            Spacer(Modifier.width(8.dp))
            DanmakuInputEmojiButton(runtime)
        }
        if (failed) {
            Text(
                text = "发送失败，请确认已登录后重试",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 5.dp),
            )
        }
    }
}
