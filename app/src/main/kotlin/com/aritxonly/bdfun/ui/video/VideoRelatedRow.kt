package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.VideoItem

@Composable
internal fun RelatedRow(
    item: VideoItem,
    onClick: () -> Unit,
    metaLabel: String? = null,
) {
    val context = LocalContext.current
    val oneTake = rememberOneTakeAnchor()
    val rowRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
    val coverWidth = 150.dp
    val coverHeight = coverWidth * 9f / 16f
    Row(
        modifier = oneTake.modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                oneTake.open(context, item, rowRadiusPx) { onClick() }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        VideoRelatedCover(item, coverWidth, coverHeight)
        Spacer(Modifier.width(10.dp))
        // 右列高度锁成封面高度：标题第一行贴封面顶部，UP 行贴封面底部
        VideoRelatedMetadata(item, metaLabel, coverHeight, Modifier.weight(1f))
    }
}


/**
 * 「自动」清晰度：按系统给出的当前网络下行带宽选一档。
 * 不看 WiFi/移动网络类型，只按实际带宽走；带宽未知时保守选 720P。
 */
