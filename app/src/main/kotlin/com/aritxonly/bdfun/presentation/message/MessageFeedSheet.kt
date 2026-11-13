package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.modal.TeakActionSheet

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.runtime.Composable

@Composable
internal fun MessageFeedSheet(
    kind: MessageFeedKind,
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageFeedUiState,
    menu: MessageFeedMenuStateHolder,
    onMutate: (Boolean) -> Unit,
) {
    val busy = snapshot.busy
    val loadingByTab = snapshot.loadingByTab
    TeakActionSheet(menu.selected != null, onDismiss = { if (!busy) menu.selected = null }, title = "消息操作") {
        val target = menu.selected
        if (target != null) {
            val enabled = !busy && loadingByTab[target.first] != true
            if (menu.confirmDelete) {
                ActionSheetRow("确认删除这条通知", enabled, MiuixIcons.Delete) {
                    onMutate(true)
                }
                ActionSheetRow("取消", enabled) { menu.confirmDelete = false }
            } else {
                if (kind == MessageFeedKind.Like) ActionSheetRow(if (target.second.noticeState == 0) "不再通知" else "接收通知", enabled) {
                    onMutate(false)
                }
                ActionSheetRow("删除通知", enabled, MiuixIcons.Delete) { menu.confirmDelete = true }
            }
        }
    }
}
