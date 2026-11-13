package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.feature.library.application.*

internal class LibraryPresentationStateHolder(chrome: PageChromePresentationState) {
    var consumedScrollTop by mutableIntStateOf(chrome.scrollTopRequest)
    var consumedBatch by mutableIntStateOf(chrome.batchRequest)
    var consumedSearch by mutableIntStateOf(chrome.searchRequest)
    var consumedBatchAction by mutableIntStateOf(chrome.batchActionRequest)
    var scrollTopTick by mutableIntStateOf(0)
}
internal class LibraryGridPresentationStateHolder(tick: Int) { var seenScrollTop by mutableIntStateOf(tick) }
internal class LibraryEntrancePresentationStateHolder(started: Boolean) { var started by mutableStateOf(started) }
