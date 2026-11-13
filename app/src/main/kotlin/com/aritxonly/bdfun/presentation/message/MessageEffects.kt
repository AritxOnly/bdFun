package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.message.application.MessageFeedStateHolder
import com.aritxonly.bdfun.feature.message.application.MessageSessionsStateHolder

@Composable
internal fun MessageSessionsEffects(
    controller: MessageSessionsStateHolder,
    loggedIn: Boolean,
    ui: MessageSessionsPresentationStateHolder,
    menu: MessageSessionMenuStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val chrome = LocalPageChromeState.current
    val context = LocalContext.current
    MessageResumeEffect(scope) { if (loggedIn) controller.load() }
    LaunchedEffect(controller, loggedIn) { if (loggedIn) controller.load() else controller.reset() }
    LaunchedEffect(controller, chrome.readAllRequest) {
        if (ui.consumedReadAll == chrome.readAllRequest) return@LaunchedEffect
        ui.consumedReadAll = chrome.readAllRequest
        if (!loggedIn) return@LaunchedEffect
        menu.confirmRead = true
    }
    LaunchedEffect(chrome.moreRequest) {
        if (ui.consumedMore != chrome.moreRequest) {
            ui.consumedMore = chrome.moreRequest
            menu.showMore = true
        }
    }
}

@Composable
internal fun MessageFeedEffects(controller: MessageFeedStateHolder, ui: MessageFeedPresentationStateHolder, scope: kotlinx.coroutines.CoroutineScope) {
    LaunchedEffect(controller) { controller.loadPage(ui.tab) }
    LaunchedEffect(controller, ui.tab) {
        val key = if (ui.tab == 1) 0 else ui.tab
        if (!controller.state.value.itemsByTab.containsKey(key)) controller.loadPage(key)
    }
    MessageResumeEffect(scope) { controller.loadPage(ui.tab) }
}
