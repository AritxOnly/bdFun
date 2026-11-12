package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun MySentScreenRoute() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { com.aritxonly.bdfun.feature.sent.application.SentHistoryStateHolder(AppGraph.sentHistory.all()) }
    com.aritxonly.bdfun.ui.MySentScreen(controller) { TeakNavigator.openVideo(context, it) }
}
