package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.message.application.*
import com.aritxonly.teak.common.modal.TeakActionSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private class KeywordPresentationState {
    var delete by mutableStateOf<String?>(null)
}

@Composable
internal fun MessageKeywordContent(state: MessageToolsUiState, controller: MessageToolsStateHolder, scope: CoroutineScope) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val ui = remember(controller) { KeywordPresentationState() }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        val keywords = state.keywords
        Text("${keywords?.keywords?.size ?: 0} / ${keywords?.listLimit ?: 0}", color = colors.onBackgroundVariant)
        if (keywords?.limitText?.isNotBlank() == true) Text(keywords.limitText, color = colors.onBackgroundVariant, fontSize = 12.sp)
        Row(Modifier.padding(vertical = 16.dp)) {
            BasicTextField(state.draft, controller::draft, enabled = !state.busy,
                modifier = Modifier.weight(1f).padding(12.dp), textStyle = TextStyle(color = colors.onBackground, fontSize = 16.sp),
                decorationBox = { input -> if (state.draft.isEmpty()) Text("输入消息屏蔽词", color = colors.onBackgroundVariant); input() })
            Text("添加", color = colors.primary, modifier = Modifier.clickable(enabled = !state.busy && !state.loading) {
                scope.launch { controller.addKeyword()?.let(context::showToast) }
            }.padding(12.dp))
        }
        keywords?.keywords?.forEach { keyword ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(keyword, color = colors.onBackground, modifier = Modifier.weight(1f).padding(12.dp))
                Text("删除", color = colors.primary, modifier = Modifier.clickable(enabled = !state.busy) { ui.delete = keyword }.padding(12.dp))
            }
        }
    }
    TeakActionSheet(ui.delete != null, onDismiss = { if (!state.busy) ui.delete = null }, title = "删除屏蔽词") {
        ActionSheetRow("确认删除「${ui.delete.orEmpty()}」", !state.busy) {
            val keyword = ui.delete ?: return@ActionSheetRow
            scope.launch { controller.deleteKeyword(keyword)?.let(context::showToast); ui.delete = null }
        }
    }
}
