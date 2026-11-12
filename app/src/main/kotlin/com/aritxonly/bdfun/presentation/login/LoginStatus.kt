package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.feature.login.application.QrLoginState

@Composable
internal fun LoginStatus(state: QrLoginState, onRefresh: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Spacer(Modifier.height(16.dp))
    Text(
        text = state.status,
        color = colors.onBackgroundVariant,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )

    if (state.expired) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.primary)
                .clickable(onClick = onRefresh)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(text = "刷新二维码", color = Color.White, fontSize = 14.sp)
        }
    }
}
