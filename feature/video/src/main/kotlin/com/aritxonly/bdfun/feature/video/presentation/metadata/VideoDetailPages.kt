package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoPage

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailPages(
    detail: VideoDetail?, metaReady: Boolean, currentCid: Long, onPage: (VideoPage) -> Unit,
) {
    val detailForPages = detail
    if (detailForPages != null && detailForPages.pages.size > 1) {
        item(key = "pages-card") {
            val enter = rememberStaggerProgress(metaReady, 7)
            // 分 P 不要整体背景：每个 P 自己一张白色圆角卡片。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                    .padding(vertical = 8.dp)
                    .graphicsLayer {
                        alpha = enter
                        translationY = (1f - enter) * 12.dp.toPx()
                    },
            ) {
                VideoPageStrip(
                    pages = detailForPages.pages,
                    currentCid = currentCid,
                    onSelect = { onPage(it) },
                )
            }
        }
    }
}
