package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.feature.sent.application.*

@Composable
internal fun SentHistoryHeader() {
    val pageChrome = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Spacer(Modifier.height(52.dp))
        Text(
            text = "我的评论与弹幕",
            modifier = Modifier
                .padding(start = 10.dp)
                .graphicsLayer {
                    val p = (pageChrome.collapse / 0.65f).coerceIn(0f, 1f)
                    alpha = 1f - p
                }
                .blur((12f * (pageChrome.collapse / 0.65f).coerceIn(0f, 1f)).dp),
            color = colors.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "仅记录在本 App 内发出的评论与弹幕",
            modifier = Modifier.padding(start = 12.dp),
            color = colors.onBackgroundVariant,
            fontSize = 11.5.sp,
        )
    }
}
