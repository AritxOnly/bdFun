package com.aritxonly.bdfun.ui

import android.content.Context
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.feed.DynamicItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DynamicFeedActions(
    private val state: DynamicFeedState,
    private val scope: CoroutineScope,
    private val context: Context,
    private val loggedIn: Boolean,
    val onOpenVideo: (VideoItem) -> Unit,
    val onOpenDynamic: (DynamicItem) -> Unit,
) {
    fun open(item: DynamicItem) { state.markSeen(item.mid); onOpenDynamic(item) }
    fun like(item: DynamicItem) {
        scope.launch {
            when (val result = state.toggleLike(item)) {
                is Outcome.Success -> context.showToast(if (result.value) "点赞成功" else "已取消赞")
                is Outcome.Failure -> context.showToast("点赞失败")
                null -> Unit
            }
        }
    }
    fun loadMore() { scope.launch { state.loadMore(loggedIn) } }
    fun retry() { scope.launch { state.retry(loggedIn) } }
}
