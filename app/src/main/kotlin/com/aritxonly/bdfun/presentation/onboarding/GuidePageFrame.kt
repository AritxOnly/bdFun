package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakTopOverlay
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuidePageFrame(
    onBack: (() -> Unit)?,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val backdrop = rememberLayerBackdrop {
        drawRect(colors.background)
        drawContent()
    }
    CompositionLocalProvider(LocalTeakBackdrop provides backdrop) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
            if (onBack != null) {
                TeakTopOverlay(revealed = true, onBack = onBack)
            }
        }
    }
}
