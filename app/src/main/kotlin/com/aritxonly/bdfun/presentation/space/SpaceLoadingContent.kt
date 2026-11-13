package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.TeakLoading

@Composable
internal fun SpaceLoadingContent(
    content: SpaceScreenContent, layout: SpaceScreenLayout, actions: SpaceScreenActions,
) {
    val banner = content.banner
    Column(modifier = Modifier.fillMaxSize()) {
        banner.bitmap?.let { SpaceBanner(bitmap = it) }
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            TeakLoading(size = 44.dp)
        }
    }
}
