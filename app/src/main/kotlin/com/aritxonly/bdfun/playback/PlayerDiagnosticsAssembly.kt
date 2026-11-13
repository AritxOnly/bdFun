package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberPlayerDiagnostics(): PlayerDiagnostics {
    val context = LocalContext.current
    val log = LocalVideoPageState.current.dependencies.log
    return remember(context, log) { PlayerDiagnostics(log) { context.showToast(it) } }
}
