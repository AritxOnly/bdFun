package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.library.application.*

internal data class LibrarySearchContent(val title: String, val history: List<String>, val results: List<VideoItem>, val loading: Boolean, val error: String?)
internal class LibrarySearchActions(val onClearHistory: () -> Unit, val onSubmit: (String) -> Unit, val onBack: () -> Unit, val onOpenVideo: (VideoItem) -> Unit)
internal class LibrarySearchEvents(private val state: LibrarySearchStateHolder, private val actions: LibrarySearchActions) {
    fun changeDraft(value: String) = state.changeDraft(value)
    fun submit() { state.submit()?.let(actions.onSubmit) }
    fun chooseHistory(word: String) { actions.onSubmit(state.chooseHistory(word)) }
    fun clearInput() = state.clearInput()
    fun clearHistory() { actions.onClearHistory(); state.clearHistoryDraft() }
}
