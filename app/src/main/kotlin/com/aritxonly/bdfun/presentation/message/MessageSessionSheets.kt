package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.modal.TeakActionSheet

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import com.aritxonly.bdfun.core.domain.SessionAction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageSessionSheets(
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState,
    menu: MessageSessionMenuStateHolder,
    onRefresh: () -> Unit,
    onReadAll: () -> Unit,
    onMutate: (SessionAction) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val loading = snapshot.loading
    val busy = snapshot.busy
    TeakActionSheet(menu.showMore, onDismiss = { menu.showMore = false }, title = "消息") {
        ActionSheetRow("刷新消息", !loading && !busy) { menu.showMore = false; onRefresh() }
        ActionSheetRow("全部标为已读", !loading && !busy) { menu.showMore = false; onReadAll() }
    }
    TeakActionSheet(menu.selected != null, onDismiss = { if (!busy) menu.selected = null }, title = menu.selected?.name) {
        val target = menu.selected
        if (target != null) {
            val enabled = !busy && !loading
            if (menu.confirmDelete) {
                Text("删除会话后，可通过对方的个人空间重新发起私信。", Modifier.padding(18.dp), color = colors.onBackgroundVariant)
                ActionSheetRow("确认删除会话", enabled, MiuixIcons.Delete) { onMutate(SessionAction.Delete) }
                ActionSheetRow("取消", enabled) { menu.confirmDelete = false }
            } else {
                ActionSheetRow(if (target.pinned) "取消置顶" else "置顶会话", enabled) {
                    onMutate(SessionAction.Pin)
                }
                if (target.sessionType == 1) ActionSheetRow(if (target.muted) "关闭免打扰" else "消息免打扰", enabled) {
                    onMutate(SessionAction.Mute)
                }
                ActionSheetRow("标为已读", enabled && target.unread > 0) {
                    onMutate(SessionAction.Read)
                }
                ActionSheetRow("删除会话", enabled, MiuixIcons.Delete) { menu.confirmDelete = true }
            }
        }
    }
}
