package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 动态 / 我的 页的顶栏：和首页同一套渐进式模糊，中间标题随内容滚动淡入。
 * 挂在 App 层（采集层之外），所以能安全读 backdrop 做真模糊。
 */
@Composable
fun TabTopBar(
    title: String,
    collapse: Float,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeTopBarHeight + statusBarTop),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .teakBarBlur(),
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = statusBarTop)
                .graphicsLayer { alpha = collapse.coerceIn(0f, 1f) },
        )
    }
}
