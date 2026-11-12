package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoPage

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailCollection(
    detail: VideoDetail?, metaReady: Boolean, currentCid: Long, onPage: (VideoPage) -> Unit,
) {
    val detailForPages = detail
    if (detailForPages != null && detailForPages.episodes.size > 1) {
        item(key = "collection-card") {
            val enter = rememberStaggerProgress(metaReady, 8)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                    .graphicsLayer {
                        alpha = enter
                        translationY = (1f - enter) * 12.dp.toPx()
                    },
            ) {
                VideoCollectionCard(
                    title = detailForPages.seasonTitle.ifBlank { detailForPages.title },
                    episodes = detailForPages.episodes,
                    currentCid = currentCid,
                    onSelect = { episode ->
                        val index = detailForPages.episodes
                            .indexOf(episode)
                            .coerceAtLeast(0)
                        onPage(
                            com.aritxonly.bdfun.data.VideoPage(
                                cid = episode.cid,
                                page = index + 1,
                                part = episode.title,
                                duration = episode.duration,
                            ),
                        )
                    },
                )
            }
        }
    }

}
