package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.TeakScrollContainer

@Composable
internal fun BottomSearchLanding(
    history: List<String>,
    hot: List<String>,
    query: String,
    suggestions: List<String>,
    onPickKeyword: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    TeakScrollContainer(listState, Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 40.dp),
        ) {
            if (query.isNotBlank()) {
                if (suggestions.isNotEmpty()) {
                    item(key = "suggest-title") {
                        BottomSectionTitle("搜索建议", null, null)
                    }
                    item(key = "suggest-list") {
                        BottomSearchSuggestions(suggestions, onPickKeyword)
                    }
                }
                return@LazyColumn
            }

            if (history.isNotEmpty()) {
                item(key = "history-title") {
                    BottomSectionTitle("搜索记录", "清空", onClearHistory)
                }
                item(key = "history-list") {
                    BottomSearchHistory(history, onPickKeyword)
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (hot.isNotEmpty()) {
                item(key = "hot-title") {
                    BottomSectionTitle("大家都在搜", null, null)
                }
                item(key = "hot-list") {
                    BottomSearchHot(hot, onPickKeyword)
                }
            }
        }
    }
}
