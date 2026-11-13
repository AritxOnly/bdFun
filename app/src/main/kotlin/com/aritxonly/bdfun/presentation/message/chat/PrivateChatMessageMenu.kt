package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.model.message.ChatMessage
import com.aritxonly.bdfun.feature.message.application.PrivateChatStateHolder
import com.aritxonly.bdfun.ui.ActionSheetRow
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.teak.common.modal.TeakActionSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalChatMessageMenu = staticCompositionLocalOf<(ChatMessage) -> Unit> { {} }
internal class ChatMessageMenuState {
    var selected by mutableStateOf<ChatMessage?>(null)
    var report by mutableStateOf(false)
    var description by mutableStateOf("")
    var busy by mutableStateOf(false)
    var image by mutableStateOf<String?>(null)
}

@Composable
internal fun PrivateChatMessageMenu(ui: ChatMessageMenuState, controller: PrivateChatStateHolder, scope: CoroutineScope) {
    val context = LocalContext.current
    TeakActionSheet(ui.selected != null, onDismiss = { if (!ui.busy) { ui.selected = null; ui.report = false } }, title = if (ui.report) "举报消息" else "消息操作") {
        val message = ui.selected ?: return@TeakActionSheet
        if (message.isMine) {
            ActionSheetRow("撤回", !message.withdrawn && !ui.busy) {
                ui.busy = true
                scope.launch {
                    try { controller.withdraw(message)?.let(context::showToast); ui.selected = null }
                    finally { ui.busy = false }
                }
            }
        } else if (!ui.report) ActionSheetRow("举报", !ui.busy) { ui.report = true }
        else {
            Column {
                Text("补充说明（选择“其他问题”时必填）", color = MiuixTheme.colorScheme.onBackgroundVariant)
                BasicTextField(ui.description, { ui.description = it }, enabled = !ui.busy,
                    textStyle = androidx.compose.ui.text.TextStyle(color = MiuixTheme.colorScheme.onBackground))
            }
            listOf(1 to "色情低俗", 2 to "政治敏感", 3 to "违法有害", 4 to "广告骚扰", 5 to "人身攻击", 6 to "诈骗", 0 to "其他问题").forEach { (reason, title) ->
                ActionSheetRow(title, !ui.busy && (reason != 0 || ui.description.isNotBlank())) {
                    ui.busy = true
                    scope.launch {
                        try { controller.report(message, reason, ui.description.ifBlank { title })?.let(context::showToast); ui.selected = null; ui.report = false }
                        finally { ui.busy = false }
                    }
                }
            }
        }
    }
}
