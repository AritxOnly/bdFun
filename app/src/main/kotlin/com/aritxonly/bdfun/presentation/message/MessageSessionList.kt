package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToPrivateChat
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakLargeTitle

@Composable
internal fun MessageSessionList(
    listState: androidx.compose.foundation.lazy.LazyListState,
    loggedIn: Boolean,
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState,
    ui: MessageSessionsPresentationStateHolder,
    menu: MessageSessionMenuStateHolder,
    events: MessageScreenEvents,
    onLoad: () -> Unit,
) {
    val context = LocalContext.current
    val visibleSessions = snapshot.sessions.filter { ui.tabIndex == 0 || (it.followed && !it.isSystem) }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp + rememberStatusBarTop(),
                bottom = 40.dp,
            ),
        ) {
            item(key = "header") {
                TeakLargeTitle(title = "消息")
            }

            item(key = "shortcuts") { MessageShortcutGroup(snapshot.unread, events) }
            item(key = "tabs") { MessageSessionTabs(ui) }
            if (!loggedIn) {
                item(key = "login") { MessageLoginPrompt(events.onNeedLogin) }
            } else {
                itemsIndexed(visibleSessions, key = { _, session -> session.id }) { index, session ->
                    MessageRowSurface(index == 0, index == visibleSessions.lastIndex) {
                        SessionRow(session, onClick = {
                            if (session.talkerId > 0L) navigateToPrivateChat(context, session)
                        }, onLongClick = { menu.selected = session; menu.confirmDelete = false })
                    }
                }
                item(key = "footer") {
                    MessagePageFooter(snapshot.loading, snapshot.error, snapshot.next != null, visibleSessions.isEmpty(), onLoad = onLoad)
                }
            }
        }
    }
}
