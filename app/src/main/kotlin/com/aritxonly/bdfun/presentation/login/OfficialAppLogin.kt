package com.aritxonly.bdfun.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OfficialAppLoginButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Spacer(Modifier.height(12.dp))
    Box(Modifier.clip(CircleShape).background(colors.surfaceContainerHigh)
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp)) {
        Text("跳转哔哩哔哩登录", color = if (enabled) colors.primary else colors.onBackgroundVariant, fontSize = 14.sp)
    }
}

internal fun openOfficialAppLogin(context: Context, uri: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addCategory(Intent.CATEGORY_BROWSABLE))
    } catch (_: ActivityNotFoundException) {
        context.showToast("未找到哔哩哔哩 App，请先安装或使用二维码登录")
    } catch (_: SecurityException) {
        context.showToast("无法打开哔哩哔哩 App，请使用二维码登录")
    }
}
