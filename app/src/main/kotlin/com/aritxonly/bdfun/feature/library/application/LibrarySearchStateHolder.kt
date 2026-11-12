package com.aritxonly.bdfun.feature.library.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class LibrarySearchState(val draft: String = "", val submitted: Boolean = false) {
    val committed get() = submitted && draft.isNotBlank()
}
internal class LibrarySearchStateHolder {
    private val mutable = MutableStateFlow(LibrarySearchState())
    val state = mutable.asStateFlow()
    fun changeDraft(value: String) { mutable.value = LibrarySearchState(value) }
    fun submit(): String? {
        val draft = mutable.value.draft
        if (draft.isBlank()) return null
        mutable.update { it.copy(submitted = true) }
        return draft
    }
    fun chooseHistory(word: String): String { mutable.value = LibrarySearchState(word, true); return word }
    fun clearHistoryDraft() { mutable.update { it.copy(draft = "") } }
    fun clearInput() { mutable.value = LibrarySearchState() }
}
