package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun TermsSheet(show: Boolean, onDismiss: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    // 和归元同一个 Miuix 底部弹层：从底部弹出、顶部有拖动条和标题、可下拉/点外面收起。
    OverlayBottomSheet(
        show = show,
        modifier = Modifier.fillMaxHeight(0.85f),
        title = "使用条款",
        backgroundColor = colors.background,
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        ) {
            TERMS_SECTIONS.forEach { section ->
                Text(
                    text = section,
                    color = colors.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
