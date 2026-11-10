package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TeakGlass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首次启动的预览版说明。玻璃卡片 + 两个胶囊按钮：我已知晓 / 退出。
 */
@Composable
fun PreviewNoticeDialog(
    onAccept: () -> Unit,
    onExit: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(interactionSource = null, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        TeakGlass(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = ContinuousCornerShape(26.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                Text(
                    text = "bdFun 预览版说明",
                    color = colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "当前版本为先行预览版本，不代表最终品质。已完成界面如发现异常，欢迎在组织内反馈相关问题；尚未完成部分，欢迎提出设计建议。如您认可本应用，可以向朋友推荐，但请避免在公共社交媒体上进行刻意宣传。",
                    color = colors.onBackgroundVariant,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(Capsule())
                            .background(colors.surfaceContainerHigh)
                            .clickable(onClick = onExit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "退出", color = colors.onBackgroundVariant, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(Capsule())
                            .background(BrandPink)
                            .clickable(onClick = onAccept),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "我已知晓",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
