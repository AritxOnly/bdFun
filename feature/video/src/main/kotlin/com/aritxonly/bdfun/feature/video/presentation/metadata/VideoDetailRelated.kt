package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailRelated(
    related: List<VideoItem>, relatedEnter: Float, surfaceColor: Color, onRelated: (VideoItem) -> Unit, slots: VideoDetailTailSlots,
) {
    if (related.isNotEmpty()) {
        itemsIndexed(
            items = related,
            key = { _, item -> "related-${item.bvid}" },
        ) { index, relatedItem ->
            val isFirst = index == 0
            val isLast = index == related.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (isFirst) 16.dp else 0.dp,
                topEnd = if (isFirst) 16.dp else 0.dp,
                bottomStart = if (isLast) 16.dp else 0.dp,
                bottomEnd = if (isLast) 16.dp else 0.dp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = if (isFirst) 14.dp else 0.dp,
                        bottom = if (isLast) 6.dp else 0.dp,
                    )
                    .graphicsLayer {
                        alpha = relatedEnter
                        translationY = (1f - relatedEnter) * 12.dp.toPx()
                    }
                    .clip(shape)
                    .background(surfaceColor)
                    .padding(vertical = 2.dp),
            ) {
                slots.relatedRow(relatedItem) { onRelated(relatedItem) }
            }
        }
    }}
