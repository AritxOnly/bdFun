package com.aritxonly.bdfun

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceCompactTopChrome(
    state: SpaceChromeState, onBack: () -> Unit, glyph: Color,
    actions: @Composable androidx.compose.foundation.layout.RowScope.(Color) -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    androidx.compose.runtime.CompositionLocalProvider(
        com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides state.headerProgress,
    ) {
        com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial {
            com.aritxonly.teak.common.chrome.TeakTopOverlay(
                revealed = state.headerProgress > 0f, onBack = onBack, glyphColor = glyph,
                trailing = { actions(glyph) },
                // 竖屏滚动后顶栏显示一个小标题（UP 昵称），和合并前一致
                center = {
                    val shown = ((state.headerProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    if (shown > 0.01f && state.title.isNotBlank()) {
                        Text(
                            text = state.title,
                            color = colors.onBackground,
                            fontSize = com.aritxonly.teak.common.chrome.TeakChromeDefaults.SmallTitleFontSize,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer { alpha = shown },
                        )
                    }
                },
            )
        }
    }

}
