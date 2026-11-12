package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.runtime.Composable

@Composable
internal fun RelationScreenRoute(mid: Long, fans: Boolean, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    com.aritxonly.bdfun.ui.RelationScreen(mid, fans, AppGraph.session.value?.mid, AppGraph.settings,
        createController = { frequent -> AppGraph.relationFeedStateHolder(mid, fans, frequent) },
        onBack = onBack, onOpenSpace = { navigateToSpace(context, it) })
}
