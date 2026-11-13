package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings

@Composable
internal fun BottomSearchBody(settings: SettingsBindings, request: BottomSearchRequest,
    actions: BottomSearchActions, local: BottomSearchPresentationState,
    state: com.aritxonly.bdfun.feature.search.application.SearchUiState) {
    val submitted = request.submitted
    val query = request.query
    val onQueryChange = actions.onQueryChange
    val onSubmit = actions.onSubmit
    val onCancel = actions.onCancel
    val onBackToLanding = actions.onBackToLanding
    var tab by local::tab
    var history by local::history
    val keyboard = LocalSoftwareKeyboardController.current
    val hotKeywords = state.hotKeywords
    val suggestions = state.suggestions
    val results = state.results
    val users = state.users
    val loading = state.loading
    val loadedOnce = state.loadedOnce
    val error = state.error
    val onOpenVideo = actions.onOpenVideo
    val onOpenUser = actions.onOpenUser
    if (submitted == null) {
        BottomSearchLanding(
            history = history,
            hot = if (settings.showHotSearch) hotKeywords else emptyList(),
            query = query,
            suggestions = suggestions,
            onPickKeyword = { word ->
                settings.addSearchHistory(word)
                history = settings.searchHistory
                onQueryChange(word)
                onSubmit(word)
                keyboard?.hide()
            },
            onClearHistory = {
                settings.clearSearchHistory()
                history = emptyList()
            },
        )
    } else {
        BottomSearchResults(
            tab = tab,
            onTabChange = { tab = it },
            results = results,
            users = users,
            loading = loading,
            loadedOnce = loadedOnce,
            error = error,
            onOpenVideo = onOpenVideo,
            onOpenUser = onOpenUser,
        )
    }
}
