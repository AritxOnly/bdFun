package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.ui.RelatedRow

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceVideoItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val allVideos = content.snapshot.videos.items
    val videoLoading = content.snapshot.videos.loading
    val tabEnterModifier = Modifier
    itemsIndexed(
        items = allVideos,
        key = { _, video -> "video-${video.bvid}" },
    ) { index, video ->
        SpaceSubmissionCard(
            index = index,
            isLast = index == allVideos.lastIndex,
            modifier = tabEnterModifier,
        ) {
            RelatedRow(
                item = video,
                metaLabel = com.aritxonly.bdfun.ui.formatDate(
                    video.pubdate,
                ),
                onClick = {
                    navigateToVideo(context, video)
                },
            )
        }
    }
    if (videoLoading) {
        item(key = "video-loading") { SpaceLoadingRow() }
    } else if (allVideos.isEmpty()) {
        item(key = "video-empty") { SpaceEmpty("还没有投稿") }
    }
}
