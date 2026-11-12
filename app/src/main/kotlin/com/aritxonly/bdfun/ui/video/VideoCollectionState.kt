package com.aritxonly.bdfun.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal class VideoCollectionExpansionStateHolder {
    var expanded by mutableStateOf(false)
}
internal class VideoCollectionScrollStateHolder {
    var pendingScrollTarget by mutableStateOf<Int?>(null)
    val episodeOffsets = mutableStateMapOf<Long, Int>()
}
internal class VideoCollectionPresentation(
    val expansion: VideoCollectionExpansionStateHolder,
    val scroll: VideoCollectionScrollStateHolder,
    val scrollState: androidx.compose.foundation.ScrollState,
    val currentIndex: Int,
    val scrollTo: (Int) -> Unit,
    val keepInsideCollection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
)

@Composable
internal fun rememberVideoCollectionPresentation(
    title: String, episodes: List<com.aritxonly.bdfun.data.VideoEpisode>, currentCid: Long,
): VideoCollectionPresentation {
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val expansion = remember(title, episodes.size) { VideoCollectionExpansionStateHolder() }
    val scroll = remember { VideoCollectionScrollStateHolder() }
    // 点击合集标题栏按钮时，等展开动画让列表完成测量后再滚。
    // 0 = 顶部，1 = 当前集，2 = 底部。

    val currentIndex = episodes.indexOfFirst { it.cid == currentCid }.let {
        if (it < 0) 0 else it
    }
    val scrollState = rememberScrollState()
    // 记住每一集在合集内部的纵坐标，点「定位」时能准确滚到当前这一集。

    fun centeredScrollOffset(): Int {
        val cid = episodes.getOrNull(currentIndex)?.cid ?: return 0
        val raw = scroll.episodeOffsets[cid]
            ?: with(density) { (currentIndex * 62).dp.toPx() }.toInt()
        val halfViewport = with(density) { 99.dp.toPx() }
        val halfItem = with(density) { 31.dp.toPx() }
        return (raw - halfViewport + halfItem).toInt().coerceAtLeast(0)
    }
    // 展开后默认让当前播放的这一集停在合集区域的中间。
    LaunchedEffect(expansion.expanded) {
        if (!expansion.expanded) return@LaunchedEffect
        delay(40)
        val target = scroll.pendingScrollTarget
        scroll.pendingScrollTarget = null
        val value = when (target) {
            0 -> 0
            2 -> scrollState.maxValue
            else -> centeredScrollOffset()
        }
        scrollState.animateScrollTo(value)
    }
    fun scrollCollection(target: Int) {
        scope.launch {
            if (!expansion.expanded) {
                scroll.pendingScrollTarget = target
                expansion.expanded = true
            } else {
                delay(40)
                val value = when (target) {
                    0 -> 0
                    2 -> scrollState.maxValue
                    else -> centeredScrollOffset()
                }
                scrollState.animateScrollTo(value)
            }
        }
    }
    val keepInsideCollection = rememberVideoCollectionScrollConnection()
    return VideoCollectionPresentation(expansion, scroll, scrollState, currentIndex, ::scrollCollection, keepInsideCollection)
}

@Composable
private fun rememberVideoCollectionScrollConnection(): NestedScrollConnection {
    // Miuix 负责回弹，这一层负责把回弹之后剩下的位移/惯性吃掉，
    // 否则快速一甩仍会把速度传给整页。
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = Velocity(0f, available.y)
        }
    }
}
