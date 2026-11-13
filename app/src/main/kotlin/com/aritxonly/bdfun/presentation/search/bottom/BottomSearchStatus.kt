package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BottomSearchLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        contentAlignment = Alignment.Center,
    ) { TeakLoading() }
}

@Composable
internal fun BottomSearchError(error: String) {
    val colors = MiuixTheme.colorScheme
    Text(
        text = error,
        color = colors.onBackgroundVariant,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 24.dp),
    )
}
