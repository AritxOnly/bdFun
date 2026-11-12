package com.aritxonly.bdfun

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.ui.RelatedRow

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceCollectionVideoItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val collectionVideos = content.snapshot.collection.items
    val collectionLoading = content.snapshot.collection.loading
    val tabEnterModifier = Modifier
    // 每一条都作为独立的 Lazy 项，不再把几十条一起塞进一个 Column。
    // 这样滚动时只组合屏幕内可见的几行，投稿页不会一次卡住。
    itemsIndexed(
        items = collectionVideos,
        key = { _, video -> "collection-${video.bvid}" },
    ) { index, video ->
        SpaceSubmissionCard(
            index = index,
            isLast = index == collectionVideos.lastIndex,
            modifier = tabEnterModifier,
        ) {
            RelatedRow(
                item = video,
                metaLabel = com.aritxonly.bdfun.ui.formatDate(
                    video.pubdate,
                ),
                onClick = {
                    TeakNavigator.openVideo(context, video)
                },
            )
        }
    }
    if (collectionLoading) {
        item(key = "collection-loading") { SpaceLoadingRow() }
    } else if (collectionVideos.isEmpty()) {
        item(key = "collection-empty") { SpaceEmpty("合集里还没有视频") }
    }
}
