package com.aritxonly.bdfun.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun androidx.compose.foundation.layout.BoxScope.HomeSearchContents(search: HomeTopBarSearch,
    actions: HomeTopBarActions, material: HomeTopBarMaterial) {
    val searchActive = search.active
    val searchResultsVisible = search.resultsVisible
    val searchInteraction = material.searchInteraction
    val onOpenSearchPage = actions.onOpenSearchPage
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MiuixIcons.Search,
            contentDescription = "搜索",
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        if (searchActive) {
            HomeSearchEditor(search, actions)
        } else {
            Text(
                text = "搜索视频、UP 主",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
    if (searchActive && searchResultsVisible) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = searchInteraction,
                    indication = null,
                    onClick = onOpenSearchPage,
                ),
        )
    }
}
