package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageFeedList(
    kind: MessageFeedKind, myName: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageFeedUiState,
    ui: MessageFeedPresentationStateHolder,
    menu: MessageFeedMenuStateHolder,
    actions: MessageFeedActions,
) {
    val colors = MiuixTheme.colorScheme
    val itemsByTab = snapshot.itemsByTab
    val loadingByTab = snapshot.loadingByTab
    val errorByTab = snapshot.errorByTab
    val nextByTab = snapshot.nextByTab
    val followedOverride = snapshot.followedOverride
    val followBusy = snapshot.followBusy
    val busy = snapshot.busy
    val tabs = messageFeedTabs(kind)
    val title = messageFeedTitle(kind)
    val key = if (ui.tab == 1) 0 else ui.tab
    val allItems = itemsByTab[key].orEmpty()
    val pageItems = if (ui.tab == 1) allItems.filter { it.followed } else allItems
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp + rememberStatusBarTop(), bottom = 40.dp)) {
        item(key = "header") { TeakLargeTitle(title = title) }
        item(key = "tabs") {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (tabs.isNotEmpty()) TeakPagerTabs(options = tabs, selected = ui.tab, onSelect = { ui.tab = it },
                    itemWidth = if (tabs.size > 2) 76.dp else 96.dp, backdrop = null, plain = true, position = ui.tabProgress)
                Spacer(Modifier.weight(1f))
                Text("刷新", color = colors.primary, fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = loadingByTab[key] != true && !busy) { actions.load(ui.tab) }.padding(12.dp))
            }
        }
        itemsIndexed(pageItems, key = { _, item -> "message_${item.id}" }) { index, item ->
            val onMenu = { menu.selected = key to item; menu.confirmDelete = false }
            MessageRowSurface(index == 0, index == pageItems.lastIndex) {
                when (kind) {
                    MessageFeedKind.Reply -> ReplyRow(item, myName, onClick = { actions.open(item) }, onMore = onMenu)
                    MessageFeedKind.Like -> LikeRow(item, onClick = { actions.open(item) }, onMore = onMenu)
                    MessageFeedKind.Fans -> FanRow(item, followedOverride[item.userMid] ?: item.followed,
                        item.userMid in followBusy, onFollow = { actions.toggleFollow(item) })
                    MessageFeedKind.System -> SystemNotificationRow(item, onMore = onMenu)
                }
            }
        }
        item(key = "footer") {
            MessagePageFooter(loadingByTab[key] == true, errorByTab[key], nextByTab[key] != null, pageItems.isEmpty(),
                onLoad = { actions.loadFooter(ui.tab) })
        }
    }
}
