package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoCollectionEpisodes(
    episodes: List<com.aritxonly.bdfun.data.VideoEpisode>, currentCid: Long,
    presentation: VideoCollectionPresentation, onSelect: (com.aritxonly.bdfun.data.VideoEpisode) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth().height(198.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(presentation.keepInsideCollection)
                .overScrollVertical(nestedScrollToParent = false)
                .verticalScroll(presentation.scrollState)
                .padding(horizontal = 10.dp)
                .padding(bottom = 8.dp),
        ) {
            episodes.forEachIndexed { index, episode ->
                val active = episode.cid == currentCid
                VideoCollectionEpisodeRow(episode, active, onSelect) { offset ->
                    presentation.scroll.episodeOffsets[episode.cid] = offset
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        // 上下 4dp 渐变，让列表在卡片边缘自然消失。
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.surface, colors.surface.copy(alpha = 0f)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.surface.copy(alpha = 0f), colors.surface),
                    ),
                ),
        )
    }
}
