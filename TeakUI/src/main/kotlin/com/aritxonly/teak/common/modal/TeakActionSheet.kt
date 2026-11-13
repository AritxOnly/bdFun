package com.aritxonly.teak.common.modal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close

/** 兼容旧操作入口，显示与拖拽生命周期统一交给 Deadliner Sheet。 */
@Composable
fun TeakActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    allowDismiss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    MiuixBottomSheet(
        show = visible,
        title = title ?: "操作",
        onDismissRequest = onDismiss,
        allowDismiss = allowDismiss,
        startAction = {
            IconButton(onClick = onDismiss, enabled = allowDismiss) {
                Icon(MiuixIcons.Close, contentDescription = "关闭")
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(padding).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            content = content,
        )
    }
}
