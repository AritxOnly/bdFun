package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun androidx.compose.foundation.layout.ColumnScope.DynamicViewerPager(
    imagePager: androidx.compose.foundation.pager.PagerState, images: List<String>,
    entrance: DynamicImageEntrance, dismissAnimated: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = imagePager,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = entrance.startScaleX + (1f - entrance.startScaleX) * entrance.progress
                    scaleY = entrance.startScaleY + (1f - entrance.startScaleY) * entrance.progress
                    translationX = (entrance.originCenter.x - entrance.screenWidthPx / 2f) * (1f - entrance.progress)
                    translationY = (entrance.originCenter.y - entrance.screenHeightPx / 2f) * (1f - entrance.progress)
                    this.alpha = entrance.alpha
                },
        ) { page ->
            DynamicZoomableImage(images[page], dismissAnimated)
        }
    }
}
