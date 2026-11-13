package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aritxonly.bdfun.feature.library.application.*
import kotlinx.coroutines.launch

@Composable
internal fun LibraryEffects(runtime: LibraryRuntime) {
    val interaction by androidx.compose.runtime.key(runtime.interaction) { runtime.interaction.state.collectAsState() }
    val chrome = runtime.chrome
    val ui = runtime.presentation
    LaunchedEffect(runtime.interaction, runtime.pager.currentPage) { runtime.interaction.pageChanged() }
    BackHandler { if (runtime.interaction.back()) runtime.navigation.onBack() }
    LaunchedEffect(chrome.scrollTopRequest) {
        val tick = chrome.scrollTopRequest
        if (tick != ui.consumedScrollTop) { ui.consumedScrollTop = tick; if (tick > 0) ui.scrollTopTick = tick }
    }
    LaunchedEffect(chrome.batchRequest) {
        val tick = chrome.batchRequest
        if (tick != ui.consumedBatch) { ui.consumedBatch = tick; if (tick > 0) runtime.interaction.toggleBatch() }
    }
    LaunchedEffect(chrome.searchRequest) {
        val tick = chrome.searchRequest
        if (tick != ui.consumedSearch) { ui.consumedSearch = tick; if (tick > 0) runtime.interaction.toggleSearch() }
    }
    LaunchedEffect(interaction.batchMode, interaction.selected.size, runtime.currentMode) {
        chrome.batchVisible = interaction.batchMode; chrome.batchCount = interaction.selected.size
        chrome.batchCanTransfer = runtime.currentMode == 0
    }
    DisposableEffect(chrome) { onDispose { chrome.clearBatch() } }
    LaunchedEffect(runtime.data, runtime.request.loggedIn, runtime.request.fixedMode) {
        if (!runtime.request.loggedIn) return@LaunchedEffect
        when(runtime.request.fixedMode) {
            0 -> launch { runtime.data.loadFavorites() }
            1 -> launch { runtime.data.loadHistory() }
            2 -> launch { runtime.loadWatchLater() }
            else -> { launch { runtime.data.loadFavorites() }; launch { runtime.data.loadHistory() }; launch { runtime.loadWatchLater() } }
        }
    }
    LaunchedEffect(chrome.batchActionRequest) {
        val tick = chrome.batchActionRequest
        if (tick != ui.consumedBatchAction) { ui.consumedBatchAction = tick; runtime.batchAction(chrome.batchActionKey) }
    }
}
