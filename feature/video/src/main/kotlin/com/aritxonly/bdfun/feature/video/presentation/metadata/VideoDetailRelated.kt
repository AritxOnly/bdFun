package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailRelated(
    related: List<VideoItem>, relatedEnter: Float, onRelated: (VideoItem) -> Unit, slots: VideoDetailTailSlots,
) {
    if (related.isNotEmpty()) {
        itemsIndexed(
            items = related,
            key = { _, item -> "related-${item.bvid}" },
        ) { index, relatedItem ->
            val isFirst = index == 0
            val isLast = index == related.lastIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (isFirst) 14.dp else 0.dp,
                        bottom = if (isLast) 6.dp else 0.dp,
                    )
                    .graphicsLayer {
                        alpha = relatedEnter
                        translationY = (1f - relatedEnter) * 12.dp.toPx()
                    }
                    .padding(vertical = 2.dp),
            ) {
                slots.relatedRow(relatedItem) { onRelated(relatedItem) }
            }
        }
    }
}
