package com.aritxonly.bdfun.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** 合集：点开整张圆角卡片向下展开，内部可以上下滑查看更早/更晚的单集。 */
@Composable
internal fun VideoCollectionCard(
    title: String,
    episodes: List<com.aritxonly.bdfun.data.VideoEpisode>,
    currentCid: Long,
    onSelect: (com.aritxonly.bdfun.data.VideoEpisode) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val presentation = rememberVideoCollectionPresentation(title, episodes, currentCid)
    val expansion = presentation.expansion
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface),
    ) {
        VideoCollectionHeader(title, episodes.size, presentation.currentIndex, expansion.expanded,
            onExpand = { expansion.expanded = !expansion.expanded }, onScroll = presentation.scrollTo)
        AnimatedVisibility(
            visible = expansion.expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            VideoCollectionEpisodes(episodes, currentCid, presentation, onSelect)
        }
    }
}
