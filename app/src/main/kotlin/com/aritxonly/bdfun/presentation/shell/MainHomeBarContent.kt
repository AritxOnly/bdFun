package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToMessages
import com.aritxonly.bdfun.navigateToLogin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.HomeTopBar
import com.aritxonly.bdfun.ui.HomeTopBarContent
import com.aritxonly.bdfun.ui.HomeTopBarSearch
import com.aritxonly.bdfun.ui.HomeTopBarActions
import kotlinx.coroutines.launch

@Composable
internal fun MainHomeBarContent(settings: SettingsBindings, shell: MainShellState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val ui = shell.ui
    val session = shell.feeds.session
    val unreadMessageCount = shell.feeds.unreadMessageCount
    val tabPagerState = shell.navigation.pager
    val appScope = shell.navigation.scope
    val searchChrome = shell.chrome.search
    val searchReveal = shell.searchReveal
    HomeTopBar(
        content = HomeTopBarContent(
            profile = session,
            unreadCount = unreadMessageCount,
        ),
        search = HomeTopBarSearch(
            active = ui.showSearch,
            query = ui.searchQuery,
            reveal = searchReveal,
            resultsVisible = ui.showSearch && ui.searchSubmitted != null,
        ),
        actions = HomeTopBarActions(
            onQueryChange = { ui.searchQuery = it },
            onSubmitSearch = {
            val word = ui.searchQuery.trim()
            if (word.isNotEmpty()) {
                settings.addSearchHistory(word)
                ui.searchSubmitted = word
                keyboard?.hide()
            }
        },
            onCancelSearch = {
            if (ui.searchSubmitted != null) ui.searchSubmitted = null else ui.closeSearch()
        },
            onClearQuery = { ui.searchQuery = "" },
            onSearch = {
            searchChrome.reset()
            ui.showSearch = true
            ui.searchQuery = ""
            ui.searchSubmitted = null
        },
            onOpenSearchPage = {
            // 结果页再点搜索框：回到搜索首页（历史/联想），不要直接改当前结果。
            ui.searchSubmitted = null
            ui.searchQuery = ""
            keyboard?.show()
        },
            onOpenMessages = { navigateToMessages(context) },
            onOpenProfile = {
            if (session == null) navigateToLogin(context) else appScope.launch { tabPagerState.animateScrollToPage(2) }
        },
        ),
        modifier = Modifier,
    )
}
