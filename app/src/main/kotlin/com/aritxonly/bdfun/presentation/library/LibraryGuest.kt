package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.library.application.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibraryGuest(onNeedLogin: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "登录后可查看收藏与历史",
            color = colors.onBackgroundVariant,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.primary)
                .clickable { onNeedLogin() }
                .padding(horizontal = 26.dp, vertical = 11.dp),
        ) {
            Text(
                text = "扫码登录",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
