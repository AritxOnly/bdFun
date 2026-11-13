package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun SpaceScrollMaterialEffects(
    scroll: SpaceScrollContext, progress: SpaceScrollProgress,
    onBarBlurFractionChange: (Float) -> Unit,
) {
    val wideSpace = scroll.layout.wideSpace
    val listState = scroll.listState
    val header = scroll.layout.header
    val headerMaxCollapsePx = scroll.layout.headerMaxCollapsePx
    val blurDensity = scroll.density
    val pageChrome = scroll.pageChrome
    val barBlurProgress by progress.blur
    androidx.compose.runtime.SideEffect { onBarBlurFractionChange(barBlurProgress) }
    if (wideSpace) TrackTeakPageScroll(listState, pageChrome)
    if (!wideSpace) {
        LaunchedEffect(
            header.collapsePx,
            header.heightPx,
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
        ) {
            val headerDone = headerMaxCollapsePx > 0f && header.collapsePx >= headerMaxCollapsePx - 1f
            val extra = if (headerDone) listState.firstVisibleItemScrollOffset.toFloat() else 0f
            val index = if (headerDone && listState.firstVisibleItemIndex > 0) 1 else 0
            pageChrome.update(index, header.collapsePx + extra, density = blurDensity.density)
        }
    }

}
