package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.RowScope.PrivateChatTextField(
    content: PrivateChatComposerContent, actions: PrivateChatComposerActions,
) {
    val draft = content.draft
    val onDraftChange = actions.onDraftChange
    val colors = MiuixTheme.colorScheme
    BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        modifier = Modifier.weight(1f),
        singleLine = true,
        textStyle = TextStyle(color = colors.onBackground, fontSize = 13.sp),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) {
                    Text(
                        text = "发个消息聊聊呗",
                        color = colors.outline,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
                inner()
            }
        },
    )
}
