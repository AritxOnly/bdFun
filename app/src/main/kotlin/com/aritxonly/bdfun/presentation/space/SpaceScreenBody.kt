package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SpaceScreenBody(
    content: SpaceScreenContent, layout: SpaceScreenLayout, actions: SpaceScreenActions,
) {
    val banner = content.banner
    val loading = content.snapshot.profile.loading || !content.banner.ready
    val wideSpace = layout.wideSpace
    if (loading || !banner.ready) {
        SpaceLoadingContent(content, layout, actions)
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (wideSpace) SpaceWideProfilePane(content, layout, actions)
            Column(modifier = Modifier.fillMaxHeight().weight(1f)) {
                if (!wideSpace) SpaceCompactHeader(content, layout, actions)
                SpaceContentPager(content, layout, actions, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
