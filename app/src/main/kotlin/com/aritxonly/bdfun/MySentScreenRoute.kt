package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun MySentScreenRoute() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { com.aritxonly.bdfun.feature.sent.application.SentHistoryStateHolder(AppGraph.sentHistory.all()) }
    com.aritxonly.bdfun.ui.MySentScreen(controller) { navigateToVideo(context, it) }
}
