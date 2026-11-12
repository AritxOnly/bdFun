package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.library.HistoryItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.library.application.*
import kotlinx.coroutines.delay

@Composable
internal fun LibraryFavoriteCard(video: VideoItem, index: Int, content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions) {
    val mode = layout.mode; val batchMode = content.batchMode; val selected = content.selected
    val folderVideos = content.folderVideos; val historyEntries = content.historyEntries; val watchLaterEntries = content.watchLaterEntries
    val onToggleSelected = actions.onToggleSelected; val onOpenVideo = actions.onOpenVideo
    val oneTake = rememberOneTakeAnchor()
    val itemContext = LocalContext.current
    val cardRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
    LibraryCardEntrance(index, "0-${video.bvid}", layout.entrance) {
    VideoGridCard(
        video = video,
        modifier = oneTake.modifier,
        onClick = {
            if (batchMode) {
                onToggleSelected(video.bvid)
            } else {
                actions.onPrepareQueue(folderVideos, video.bvid)
                oneTake.open(itemContext, video, cardRadiusPx) { onOpenVideo(video) }
            }
        },
        selectionEnabled = batchMode,
        selected = video.bvid in selected,
    )
    }
}

@Composable
internal fun LibraryHistoryCard(entry: HistoryItem, index: Int, content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions) {
    val mode = layout.mode; val batchMode = content.batchMode; val selected = content.selected
    val folderVideos = content.folderVideos; val historyEntries = content.historyEntries; val watchLaterEntries = content.watchLaterEntries
    val onToggleSelected = actions.onToggleSelected; val onOpenVideo = actions.onOpenVideo
    val video = entry.toVideoItem()
    val oneTake = rememberOneTakeAnchor()
    val itemContext = LocalContext.current
    val cardRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
    LibraryCardEntrance(index, "${mode}-${entry.bvid}", layout.entrance) {
    VideoGridCard(
        video = video,
        modifier = oneTake.modifier,
        onClick = {
            if (batchMode) {
                onToggleSelected(entry.bvid)
            } else {
                val queue = if (mode == 1) {
                    historyEntries.map { it.toVideoItem() }
                } else {
                    watchLaterEntries.map { it.toVideoItem() }
                }
                actions.onPrepareQueue(queue, entry.bvid)
                oneTake.open(itemContext, video, cardRadiusPx) { onOpenVideo(video) }
            }
        },
        progress = if (entry.duration > 0) {
            entry.progress.toFloat() / entry.duration
        } else {
            0f
        },
        selectionEnabled = batchMode,
        selected = entry.bvid in selected,
    )
    }
}

@Composable
internal fun LibraryCardEntrance(
    index: Int,
    itemKey: String,
    registry: LibraryEntranceRegistry,
    content: @Composable () -> Unit,
) {
    val isNew = remember(itemKey) { registry.mark(itemKey) }
    val ui = remember(itemKey) { LibraryEntrancePresentationStateHolder(!isNew) }
    LaunchedEffect(itemKey, isNew) {
        if (isNew) {
            kotlinx.coroutines.delay(kotlin.math.min(index / 2, 6) * 70L)
            ui.started = true
        }
    }
    val progress = rememberManualProgress(if (ui.started) 1f else 0f, 140f)
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 12.dp.toPx()
        },
    ) {
        content()
    }
}
