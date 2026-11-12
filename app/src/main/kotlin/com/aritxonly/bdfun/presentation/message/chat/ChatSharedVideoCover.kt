package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.ui.formatCount
import com.aritxonly.bdfun.ui.rememberImageRequest
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.core.model.message.SharedVideoInfo

@Composable
internal fun ChatSharedVideoCover(info: SharedVideoInfo) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(colors.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = rememberImageRequest(info.cover),
            contentDescription = info.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(34.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000))),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${formatCount(info.play)} 播放",
                color = Color.White,
                fontSize = 10.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${formatCount(info.danmaku)} 弹幕",
                color = Color.White,
                fontSize = 10.sp,
            )
        }
    }
}
