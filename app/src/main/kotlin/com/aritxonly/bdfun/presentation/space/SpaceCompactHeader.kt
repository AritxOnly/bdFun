package com.aritxonly.bdfun

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged

@Composable
internal fun SpaceCompactHeader(
    content: SpaceScreenContent, layout: SpaceScreenLayout, actions: SpaceScreenActions,
) {
    val ui = content.ui
    val header = layout.header
    val headerMaxCollapsePx = layout.headerMaxCollapsePx
    // 竖屏旧逻辑：资料头部（含分段栏）在 Pager 之外，
    // 切分段时只有下方内容横向移动，头部不动。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .collapseHeaderContainer(header.heightPx, header.collapsePx, 0)
            .pointerInput(ui.selectedTab, headerMaxCollapsePx) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    actions.onPageDrag(dragAmount)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { header.heightPx = it.height }
                .graphicsLayer { translationY = -header.collapsePx },
        ) {
            SpaceCompactProfile(content, actions)
        }
    }
}
