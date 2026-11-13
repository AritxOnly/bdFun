package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.domain.SearchFilterState

internal class SearchPresentationStateHolder(history: List<String>) {
    var tab by mutableStateOf(0)
    var filterOpen by mutableStateOf(false)
    var filters by mutableStateOf(SearchFilterState())
    var appliedFilters by mutableStateOf(SearchFilterState())
    var history by mutableStateOf(history)
}
