package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 未完成页面的统一提示层：页面本身照常进入、可以返回，
 * 只在进入时盖一层说明，避免用户以为页面坏了。
 */
@Composable
fun UnfinishedNoticeOnce() {
    var show by remember { mutableStateOf(true) }
    if (show) {
        UnfinishedNotice(onDismiss = { show = false })
    }
}

@Composable
fun UnfinishedNotice(onDismiss: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(292.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "该页面尚未完成",
                color = colors.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "当前页面仍在制作中，后续版本会继续完善。",
                modifier = Modifier.padding(top = 8.dp),
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
            )
            Column(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BrandPink)
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "我知道了",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}
