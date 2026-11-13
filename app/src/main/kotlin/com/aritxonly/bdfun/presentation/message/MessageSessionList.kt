package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToPrivateChat
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakLargeTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageSessionList(
    listState: androidx.compose.foundation.lazy.LazyListState,
    loggedIn: Boolean,
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState,
    ui: MessageSessionsPresentationStateHolder,
    menu: MessageSessionMenuStateHolder,
    events: MessageScreenEvents,
    onLoad: () -> Unit,
    title: String = "消息", pageType: Int = 1, onRefresh: () -> Unit = {},
) {
    val context = LocalContext.current
    val visibleSessions = snapshot.sessions
    PullToRefreshBox(isRefreshing = snapshot.loading && snapshot.sessions.isNotEmpty(), onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
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
                TeakLargeTitle(title = title)
            }

            if (pageType == 1) item(key = "shortcuts") { MessageShortcutGroup(snapshot.unread, events) }
            item(key = "menus") { MessageInboxToolbar(snapshot, menu) }
            if (!loggedIn) {
                item(key = "login") { MessageLoginPrompt(events.onNeedLogin) }
            } else {
                itemsIndexed(visibleSessions, key = { _, session -> session.id }) { index, session ->
                    MessageRowSurface(index == 0, index == visibleSessions.lastIndex) {
                        SessionRow(session, onClick = {
                            when {
                                session.preferences.pageType != null -> context.startActivity(com.aritxonly.bdfun.navigation.createRouteIntent(context,
                                    com.aritxonly.bdfun.core.navigation.Route.MessageInbox(session.preferences.pageType!!, session.name)))
                                session.isSystem -> com.aritxonly.bdfun.navigateToMessageFeed(context, "system")
                                session.talkerId > 0L -> navigateToPrivateChat(context, session)
                                else -> context.showToast("该类型会话暂不支持打开")
                            }
                        }, onLongClick = { menu.selected = session; menu.confirmDelete = false })
                    }
                }
                item(key = "footer") {
                    MessagePageFooter(snapshot.loading, snapshot.error, snapshot.inboxCursor != null, visibleSessions.isEmpty(), onLoad = onLoad)
                }
            }
        }
    }
}
