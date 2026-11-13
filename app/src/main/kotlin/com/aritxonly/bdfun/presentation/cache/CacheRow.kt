package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.cache.CacheStatus
import com.aritxonly.bdfun.core.model.cache.CachedVideo
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun CacheRow(
    entry: CachedVideo,
    cover: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = entry.status == CacheStatus.Completed,
                onClick = onOpen,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CacheCover(cover)
        Spacer(Modifier.width(10.dp))
        CacheRowContent(entry)
        Spacer(Modifier.width(8.dp))
        CacheRowActions(entry.status == CacheStatus.Completed, onOpen, onDelete)
    }
}
