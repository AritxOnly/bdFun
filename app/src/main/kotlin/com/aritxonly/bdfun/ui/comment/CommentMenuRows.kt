package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun CommentMenuRow(
    label: String,
    glyph: MenuGlyphKind?,
    tint: Color,
    danger: Boolean = false,
    onClick: () -> Unit,
) {

    // 直接用设置页二级菜单同一套 Miuix 玻璃行：同样的高度、内边距、按压反馈和文字排版。
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = label,
        onClick = onClick,
        contentColor = tint,
        selected = false,
    )
}
