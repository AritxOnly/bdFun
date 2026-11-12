package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceWideTopChrome(
    state: SpaceChromeState, onBack: () -> Unit, glyph: Color, pageGlyph: Color,
    tabs: @Composable (Dp) -> Unit, actions: @Composable androidx.compose.foundation.layout.RowScope.(Color) -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    Row(Modifier.fillMaxWidth()) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides state.headerProgress,
        ) {
            com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial(modifier = Modifier.width(state.profileWidth)) {
                com.aritxonly.teak.common.chrome.TeakTopOverlay(revealed = state.headerProgress > 0f,
                    onBack = onBack, glyphColor = glyph)
            }
        }
        androidx.compose.runtime.CompositionLocalProvider(
            com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides state.contentProgress,
        ) {
            com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial(modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f)) {
                        if (state.visible) tabs(((maxWidth - 8.dp) / 4).coerceAtMost(74.dp))
                    }
                    actions(pageGlyph)
                }
            }
        }
    }

}
