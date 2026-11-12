package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.data.VideoPage

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailTail(
    detail: VideoDetail?, metaReady: Boolean, currentCid: Long, error: String?, related: List<VideoItem>,
    relatedEnter: Float, textColor: Color, surfaceColor: Color,
    onPage: (VideoPage) -> Unit, onRelated: (VideoItem) -> Unit,
) {
    videoDetailPages(detail, metaReady, currentCid, onPage)
    videoDetailCollection(detail, metaReady, currentCid, onPage)
    if (error != null) {
        item(key = "error") {
            Text(
                text = error,
                modifier = Modifier.padding(16.dp),
                color = textColor,
                fontSize = 13.sp,
            )
        }
    }

    videoDetailRelated(related, relatedEnter, surfaceColor, onRelated)
}
