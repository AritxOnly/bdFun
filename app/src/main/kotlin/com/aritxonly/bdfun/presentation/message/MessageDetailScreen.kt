package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

@Composable
fun MessageDetailScreen(kind: MessageFeedKind) {
    val context = LocalContext.current
    val owner = AppGraph.session.identity
    val controller = remember(owner, kind) { AppGraph.messageFeedStateHolder(kind) }
    val scope = rememberMessageTaskScope(controller) { controller.close() }
    val snapshot by androidx.compose.runtime.key(controller) { controller.state.collectAsState() }
    val listState = rememberLazyListState()
    val ui = remember(kind) { MessageFeedPresentationStateHolder() }
    val menu = remember(controller) { MessageFeedMenuStateHolder() }
    val actions = MessageFeedActions(controller, scope, context, menu)
    RegisterScrollTop(listState, "message_detail")
    TrackTeakPageScroll(listState)
    val myName = AppGraph.session.value?.name.orEmpty()
    MessageFeedEffects(controller, ui, scope)
    MessageFeedList(kind, myName, listState, snapshot, ui, menu, actions)
    MessageFeedSheet(kind, snapshot, menu, actions::mutate)
}
