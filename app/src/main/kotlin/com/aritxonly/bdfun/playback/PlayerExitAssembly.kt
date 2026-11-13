package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberPlayerExitHooks(): PlayerExitHooks {
    val context = LocalContext.current
    val page = LocalVideoPageState.current
    val settings = page.settings
    val background = page.dependencies.background
    return remember(context, page) {
        PlayerExitHooks({ settings.resumePlayback }, { settings.showDiscAfterVideo },
            { settings.stopOnVideoPageExit }, background::shouldHandOff,
            { background.adopt(it, context) }, background::cancelHandOff)
    }
}
