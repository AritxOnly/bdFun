package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items

internal fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.dynamicFeedItems(
    state: DynamicFeedState, actions: DynamicFeedActions, media: DynamicFeedMedia,
) {
    items(state.items, key = { it.id }, contentType = { if (it.video != null) "video" else "dynamic" }) { item ->
        val likedNow = state.liked[item.id] ?: item.liked
        val countNow = state.likeCounts[item.id] ?: item.like
        DynamicCard(
            item = item.copy(
                counts = item.counts.copy(
                    liked = likedNow,
                    like = countNow,
                ),
            ),
            actions = com.aritxonly.bdfun.ui.DynamicCardActions(
                onOpen = {
                actions.open(item)
            },
                onOpenVideo = { actions.onOpenVideo(it) },
                onLike = { actions.like(item) },
                onComment = { actions.onOpenDynamic(item) },
                onRepost = { actions.onOpenDynamic(item) },
                onOpenForwarded = { actions.onOpenDynamic(it) },
                onToggleMute = media.onToggleMute
            ),
            presentation = com.aritxonly.bdfun.ui.DynamicCardPresentation(liked = likedNow, autoPlay = media.autoPlay && media.autoPlayId == item.id, muted = media.muted),
        )
    }

}
