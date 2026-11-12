package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
internal fun rememberVideoDetailTailSlots(): VideoDetailTailSlots = remember {
    VideoDetailTailSlots(
        cover = { cover, modifier ->
            AsyncImage(model = rememberImageRequest(cover), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = modifier)
        },
        relatedRow = { item, onClick -> RelatedRow(item = item, onClick = onClick) },
    )
}
