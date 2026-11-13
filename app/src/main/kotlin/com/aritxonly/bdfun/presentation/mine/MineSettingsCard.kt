package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MineSettingsCard(onSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
        ArrowPreference(title = "设置", summary = "外观、播放与通用设置", onClick = onSettings,
            startAction = {
                Icon(MiuixIcons.Settings, contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            })
    }
}
