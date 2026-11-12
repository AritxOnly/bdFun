package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.centeredListWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchResultList(
    page: Int, presentation: SearchResultsPresentation,
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    onOpenVideo: (VideoItem) -> Unit, onOpenUser: (SearchUser) -> Unit,
) {
    val pageColors = MiuixTheme.colorScheme
    LazyColumn(
        state = if (page == 0) presentation.videos else presentation.users,
        modifier = Modifier.fillMaxSize().centeredListWidth(
            scrollState = if (page == 0) presentation.videos else presentation.users,
        ),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            // 顶栏 + Tab 栏 + 一点间距
            top = HomeTopBarHeight + 54.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        searchResultStatusItems(content, pageColors)
        if (page == 0) searchVideoResultItems(content, pageColors, onOpenVideo)
        else searchUserResultItems(content, pageColors, onOpenUser)

    }
}
