package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun LoginHeader() {
    val colors = MiuixTheme.colorScheme
    Spacer(Modifier.height(44.dp))
    Text(
        text = "扫码登录",
        color = colors.onBackground,
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "登录后可查看关注动态、个人主页与更高清晰度",
        color = colors.onBackgroundVariant,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))

}
