package com.aritxonly.bdfun.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState
import com.aritxonly.bdfun.navigation.createRouteIntent
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageInboxToolbar(snapshot: MessageSessionsUiState, menu: MessageSessionMenuStateHolder) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        snapshot.outside.forEach { item ->
            Text(item.title, color = MiuixTheme.colorScheme.primary, modifier = Modifier.clickable {
                messageMenuAction(context, item, menu)
            }.padding(12.dp))
        }
        Spacer(Modifier.weight(1f))
        Text("更多", color = MiuixTheme.colorScheme.primary, modifier = Modifier.clickable { menu.showMore = true }.padding(12.dp))
    }
}

internal fun messageMenuAction(context: Context, item: MessageMenuItem, menu: MessageSessionMenuStateHolder) {
    menu.showMore = false
    when (item.type) {
        1 -> menu.confirmRead = true
        9 -> menu.confirmClear = true
        2 -> context.startActivity(createRouteIntent(context, Route.MessageTools("settings")))
        8 -> context.startActivity(createRouteIntent(context, Route.MessageTools("contacts")))
        4 -> {
            val talker = Regex("/(\\d{3,})").find(item.url)?.groupValues?.get(1)?.toLongOrNull()
            if (talker != null) com.aritxonly.bdfun.navigateToPrivateChat(context,
                MessageSession("1_$talker", talker, 1, item.title, item.icon, "", 0, 0, false))
        }
        else -> if (item.url.startsWith("http")) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            else context.showToast("暂不支持此入口：${item.title}")
    }
}
